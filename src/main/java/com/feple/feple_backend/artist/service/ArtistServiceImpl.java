package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.ArtistNameValidator;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistUpdateFields;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.MusicGenre;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.cache.EvictArtistCaches;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService, ArtistAdminService {

    private static final int ADMIN_PAGE_SIZE = 30;

    private static Sort adminSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "name"          -> Sort.by(Direction.ASC,  "name");
            case "name_desc"     -> Sort.by(Direction.DESC, "name");
            case "followers"     -> Sort.by(Direction.DESC, "followerCount");
            case "followers_asc" -> Sort.by(Direction.ASC,  "followerCount");
            default              -> Sort.by(Direction.DESC, "weeklyScore").and(Sort.by(Direction.ASC, "id"));
        };
    }

    private final ArtistRepository artistRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final FileStorageService fileStorageService;
    private final SongRepository songRepository;
    private final ArtistNameValidator artistNameValidator;

    private ArtistResponseDto toDto(Artist artist) {
        return ArtistResponseDto.from(artist, fileStorageService.buildUrl(artist.getProfileImageKey()));
    }

    @Override
    @Transactional
    @EvictArtistCaches
    public Long createArtist(ArtistRequestDto dto) {
        Artist artist = Artist.builder()
                .name(dto.getName())
                .nameEn(dto.getNameEn())
                .aliases(parseAliases(dto.getAliases()))
                .genres(dto.getGenres())
                .profileImageKey(dto.getProfileImageKey())
                .build();
        Long id = artistRepository.save(artist).getId();
        artistNameValidator.reload();
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("allArtistsSortedByName")
    public List<ArtistResponseDto> getAllArtistsSortedByName() {
        return artistRepository.findAllByDeletedAtIsNull(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toDto).toList();
    }

    @Override
    public String uploadProfile(MultipartFile file, String artistName) throws IOException {
        return fileStorageService.storeArtistProfile(file, artistName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getFollowedArtists(Long userId) {
        return artistFollowRepository.findByUserId(userId, PageRequest.of(0, PageSize.MY_ACTIVITIES)).stream()
                .map(follow -> toDto(follow.getArtist()))
                .toList();
    }

    @Override
    @Cacheable("artistRanking")
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getAllArtists() {
        return artistRepository.findAllByDeletedAtIsNull(PageRequest.of(0, PageSize.MY_ACTIVITIES,
                        Sort.by(Sort.Direction.DESC, "weeklyScore").and(Sort.by(Sort.Direction.ASC, "id"))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 아티스트 테이블이 수백 행 규모라 LIKE 풀스캔으로도 충분히 빠름 — FULLTEXT(ngram)로
    // 전환했다가 innodb_ft_min_token_size(3)/ngram_token_size(2) 설정 불일치로 실제
    // 존재하는 영문 부분 문자열도 비결정적으로 못 찾는 회귀가 발생해 되돌림.
    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> searchArtists(String keyword) {
        String trimmed = keyword.trim();
        List<Artist> artists = artistRepository.findByNameOrNameEnContainingIgnoreCase(JpqlLikeEscaper.escape(trimmed));
        return artists.stream()
                .map(this::toDto)
                .toList();
    }

    private static boolean isSongCountSort(String sort) {
        return "songs".equals(sort) || "songs_asc".equals(sort);
    }

    private static boolean hasSearchKeyword(String keyword) {
        return keyword != null && !keyword.isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArtistResponseDto> getAdminArtistList(String sort, String keyword, MusicGenre genre, int page) {
        boolean songSort   = isSongCountSort(sort);
        boolean hasKeyword = hasSearchKeyword(keyword);

        return (hasKeyword || songSort)
                ? getAdminArtistListInMemory(sort, keyword, genre, page)
                : getAdminArtistListFromDb(sort, genre, page);
    }

    // 키워드 검색·곡수 정렬은 인메모리 처리 후 PageImpl로 슬라이싱
    private Page<ArtistResponseDto> getAdminArtistListInMemory(String sort, String keyword, MusicGenre genre, int page) {
        Map<Long, Integer> songCountMap = buildSongCountMap();
        List<Artist> artists = hasSearchKeyword(keyword)
                ? artistRepository.findByNameOrNameEnContainingIgnoreCase(JpqlLikeEscaper.escape(keyword.trim()))
                : artistRepository.findAllByDeletedAtIsNull();
        if (genre != null) {
            artists = artists.stream().filter(a -> a.getGenres().contains(genre)).toList();
        }
        List<ArtistResponseDto> dtos = artists.stream()
                .map(a -> toAdminDto(a, songCountMap))
                .collect(Collectors.toCollection(ArrayList::new));
        if ("songs".equals(sort)) {
            dtos.sort(Comparator.comparingInt(ArtistResponseDto::getSongCount).reversed());
        } else if ("songs_asc".equals(sort)) {
            dtos.sort(Comparator.comparingInt(ArtistResponseDto::getSongCount));
        }
        int start = page * ADMIN_PAGE_SIZE;
        int end   = Math.min(start + ADMIN_PAGE_SIZE, dtos.size());
        return new PageImpl<>(start >= dtos.size() ? List.of() : dtos.subList(start, end),
                PageRequest.of(page, ADMIN_PAGE_SIZE), dtos.size());
    }

    // 일반 케이스: DB 레벨 페이지네이션
    private Page<ArtistResponseDto> getAdminArtistListFromDb(String sort, MusicGenre genre, int page) {
        PageRequest pageable = PageRequest.of(page, ADMIN_PAGE_SIZE, adminSort(sort));
        Page<Artist> artistPage = (genre != null)
                ? artistRepository.findByGenreName(genre.name(), pageable)
                : artistRepository.findAllByDeletedAtIsNull(pageable);
        List<Long> artistIds = artistPage.getContent().stream().map(Artist::getId).toList();
        Map<Long, Integer> songCountMap = buildSongCountMapForIds(artistIds);
        return artistPage.map(a -> toAdminDto(a, songCountMap));
    }

    private ArtistResponseDto toAdminDto(Artist artist, Map<Long, Integer> songCountMap) {
        return ArtistResponseDto.from(artist,
                fileStorageService.buildUrl(artist.getProfileImageKey()),
                songCountMap.getOrDefault(artist.getId(), 0));
    }

    private Map<Long, Integer> buildSongCountMap() {
        return QueryResultMapper.toIntMap(songRepository.countGroupedByArtist());
    }

    private Map<Long, Integer> buildSongCountMapForIds(List<Long> artistIds) {
        if (artistIds.isEmpty()) return Map.of();
        return QueryResultMapper.toIntMap(songRepository.countGroupedByArtistIds(artistIds));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "artistDetail", key = "#id")
    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findByIdAndDeletedAtIsNull, id, "아티스트");
        return toDto(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistRequestDto getArtistForEdit(Long id) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, id, "아티스트");
        return ArtistRequestDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .nameEn(artist.getNameEn())
                .aliases(artist.getAliasesDisplay())
                .genres(artist.getGenres())
                .profileImageKey(fileStorageService.buildUrl(artist.getProfileImageKey()))
                .followerCount(artist.getFollowerCount())
                .build();
    }

    @Override
    @Transactional
    @EvictArtistCaches
    @CacheEvict(value = "artistDetail", key = "#id")
    public void updateArtist(Long id, ArtistRequestDto dto) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, id, "아티스트");
        artist.update(new ArtistUpdateFields(dto.getName(), dto.getNameEn(), dto.getGenres(), parseAliases(dto.getAliases())));
        if (dto.getProfileImageKey() != null) {
            String oldKey = artist.getProfileImageKey();
            artist.updateProfileImage(dto.getProfileImageKey());
            if (oldKey != null) {
                fileStorageService.deleteFileAfterCommit(oldKey);
            }
        }
        artistNameValidator.reload();
    }

    @Override
    @Cacheable(value = "topArtists", key = "#limit")
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getTopArtists(int limit) {
        return artistRepository.findAllByDeletedAtIsNull(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "followerCount")))
                .getContent().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    @EvictArtistCaches
    @CacheEvict(value = "artistDetail", key = "#id")
    public void updateArtistPhoto(Long id, String imageKey) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, id, "아티스트");
        String oldKey = artist.getProfileImageKey();
        artist.updateProfileImage(imageKey);
        if (oldKey != null) {
            fileStorageService.deleteFileAfterCommit(oldKey);
        }
    }

    @Override
    @Transactional
    @EvictArtistCaches
    @CacheEvict(value = "artistDetail", key = "#id")
    public void deleteArtist(Long id) {
        // 소프트 삭제 — 라인업·팔로우·곡 등 연관 데이터는 그대로 두고 목록·검색에서만 제외한다.
        // 물리적으로 row가 남아있어 기존 FK 참조(게시글·타임테이블 등)도 깨지지 않고,
        // 관리자가 휴지통에서 그대로 복구할 수 있다.
        Artist artist = EntityLoader.getOrThrow(artistRepository::findByIdAndDeletedAtIsNull, id, "아티스트");
        artist.softDelete();
        artistNameValidator.reload();
    }

    @Override
    @Transactional
    @EvictArtistCaches
    @CacheEvict(value = "artistDetail", key = "#id")
    public void restoreArtist(Long id) {
        artistRepository.restoreById(id);
        artistNameValidator.reload();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getDeletedArtists() {
        return artistRepository.findSoftDeleted().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getRelatedArtists(Long artistId, int limit) {
        List<Long> festivalIds = artistFestivalRepository
                .findByArtistIdOrderByFestivalStartDateAsc(artistId)
                .stream()
                .map(ArtistFestival::getFestivalId)
                .toList();
        if (festivalIds.isEmpty()) return List.of();

        Map<Long, Long> coAppearanceCount = computeCoAppearanceCounts(artistId, festivalIds);
        if (coAppearanceCount.isEmpty()) return List.of();

        return topRelatedArtists(coAppearanceCount, limit);
    }

    // 같은 페스티벌에 출연한 다른 아티스트의 공동 출연 횟수 집계
    private Map<Long, Long> computeCoAppearanceCounts(Long artistId, List<Long> festivalIds) {
        return artistFestivalRepository
                .findByFestivalIdInWithArtist(festivalIds)
                .stream()
                .filter(af -> !af.getArtistId().equals(artistId))
                .collect(Collectors.groupingBy(ArtistFestival::getArtistId, Collectors.counting()));
    }

    // 삭제된 아티스트를 먼저 걸러낸 뒤 상위 limit개를 뽑아야 한다 — 순서가 반대면
    // (상위 limit개를 먼저 뽑고 그중 삭제된 것을 제외하면) 삭제된 아티스트가
    // 상위권을 차지했을 때 최종 결과가 limit보다 적게 나온다.
    private List<ArtistResponseDto> topRelatedArtists(Map<Long, Long> coAppearanceCount, int limit) {
        Map<Long, Artist> artistMap = artistRepository.findAllById(coAppearanceCount.keySet()).stream()
                .filter(a -> !a.isDeleted())
                .collect(Collectors.toMap(Artist::getId, a -> a));

        return coAppearanceCount.entrySet().stream()
                .filter(e -> artistMap.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(e -> artistMap.get(e.getKey()))
                .map(this::toDto)
                .toList();
    }

    @Override
    public long getTotalCount() {
        return artistRepository.countByDeletedAtIsNull();
    }

    // artist_aliases.alias 컬럼 길이(VARCHAR(200))와 일치 — 콤마 없는 단일 별명이 DTO 전체 길이
    // 제한(500자)은 통과하고도 컬럼 길이를 넘어 저장 시 truncation 오류가 나는 것을 막는다
    private static final int ALIAS_MAX_LENGTH = 200;

    private List<String> parseAliases(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> aliases = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        aliases.stream().filter(alias -> alias.length() > ALIAS_MAX_LENGTH).findFirst().ifPresent(alias -> {
            throw new IllegalArgumentException("별명은 각 항목당 " + ALIAS_MAX_LENGTH + "자 이하여야 합니다.");
        });
        return aliases;
    }
}
