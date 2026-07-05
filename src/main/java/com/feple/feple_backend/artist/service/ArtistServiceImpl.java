package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.ArtistNameFilter;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.CountRowMapper;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictArtistCaches;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort.Direction;

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
    private final ArtistCascadeDeleteService cascadeDeleteService;
    private final SongRepository songRepository;
    private final ArtistNameFilter artistNameFilter;

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
                .aliases(dto.getAliases())
                .genres(dto.getGenres())
                .profileImageKey(dto.getProfileImageKey())
                .build();
        Long id = artistRepository.save(artist).getId();
        artistNameFilter.reload();
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("allArtistsSortedByName")
    public List<ArtistResponseDto> getAllArtistsSortedByName() {
        return artistRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
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
        return artistRepository.findAll(PageRequest.of(0, PageSize.MY_ACTIVITIES,
                        Sort.by(Sort.Direction.DESC, "weeklyScore").and(Sort.by(Sort.Direction.ASC, "id"))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> searchArtists(String keyword) {
        return artistRepository.searchArtistsByNameFullText(keyword.trim()).stream()
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
    public Page<ArtistResponseDto> getAdminArtistList(String sort, String keyword, ArtistGenre genre, int page) {
        boolean songSort   = isSongCountSort(sort);
        boolean hasKeyword = hasSearchKeyword(keyword);

        if (hasKeyword || songSort) {
            // 키워드 검색·곡수 정렬은 인메모리 처리 후 PageImpl로 슬라이싱
            Map<Long, Integer> songCountMap = buildSongCountMap();
            List<Artist> artists = hasKeyword
                    ? artistRepository.findByNameOrNameEnContainingIgnoreCase(LikeEscaper.escape(keyword.trim()))
                    : artistRepository.findAll();
            if (genre != null) {
                artists = artists.stream().filter(a -> a.getGenres().contains(genre)).toList();
            }
            List<ArtistResponseDto> dtos = artists.stream()
                    .map(a -> ArtistResponseDto.from(a,
                            fileStorageService.buildUrl(a.getProfileImageKey()),
                            songCountMap.getOrDefault(a.getId(), 0)))
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
        PageRequest pageable = PageRequest.of(page, ADMIN_PAGE_SIZE, adminSort(sort));
        Page<Artist> artistPage = (genre != null)
                ? artistRepository.findByGenreName(genre.name(), pageable)
                : artistRepository.findAll(pageable);
        List<Long> artistIds = artistPage.getContent().stream().map(Artist::getId).toList();
        Map<Long, Integer> songCountMap = buildSongCountMapForIds(artistIds);
        return artistPage.map(a -> ArtistResponseDto.from(a,
                fileStorageService.buildUrl(a.getProfileImageKey()),
                songCountMap.getOrDefault(a.getId(), 0)));
    }

    private Map<Long, Integer> buildSongCountMap() {
        return CountRowMapper.toIntMap(songRepository.countGroupedByArtist());
    }

    private Map<Long, Integer> buildSongCountMapForIds(List<Long> artistIds) {
        if (artistIds.isEmpty()) return Map.of();
        return CountRowMapper.toIntMap(songRepository.countGroupedByArtistIds(artistIds));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "artistDetail", key = "#id")
    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        return toDto(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistRequestDto getArtistForEdit(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        return ArtistRequestDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .nameEn(artist.getNameEn())
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
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        artist.update(dto.getName(), dto.getNameEn(), dto.getGenres(), dto.getAliases());
        if (dto.getProfileImageKey() != null) {
            String oldKey = artist.getProfileImageKey();
            artist.updateProfileImage(dto.getProfileImageKey());
            if (oldKey != null) {
                fileStorageService.deleteFileAfterCommit(oldKey);
            }
        }
        artistNameFilter.reload();
    }

    @Override
    @Cacheable(value = "topArtists", key = "#limit")
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getTopArtists(int limit) {
        return artistRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "followerCount")))
                .getContent().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "artistRanking", allEntries = true),
        @CacheEvict(value = "topArtists", allEntries = true),
        @CacheEvict(value = "artistDetail", key = "#id")
    })
    public void updateArtistPhoto(Long id, String imageKey) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        String oldKey = artist.getProfileImageKey();
        artist.updateProfileImage(imageKey);
        if (oldKey != null) {
            fileStorageService.deleteFileAfterCommit(oldKey);
        }
    }

    @Override
    @Transactional
    @EvictArtistCaches
    public void batchUpdateNameEn(List<Long> ids, List<String> nameEns) {
        Map<Long, Artist> artistMap = artistRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Artist::getId, a -> a));
        for (int i = 0; i < ids.size(); i++) {
            Artist artist = artistMap.get(ids.get(i));
            if (artist != null) {
                String nameEn = (i < nameEns.size()) ? nameEns.get(i).trim() : "";
                artist.updateNameEn(nameEn.isEmpty() ? null : nameEn);
            }
        }
    }

    @Override
    @Transactional
    @EvictArtistCaches
    @CacheEvict(value = "artistDetail", key = "#id")
    public void deleteArtist(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        cascadeDeleteService.delete(artist);
        artistNameFilter.reload();
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

        // 같은 페스티벌에 출연한 다른 아티스트의 공동 출연 횟수 집계
        Map<Long, Long> coAppearanceCount = artistFestivalRepository
                .findByFestivalIdInWithArtist(festivalIds)
                .stream()
                .filter(af -> !af.getArtistId().equals(artistId))
                .collect(Collectors.groupingBy(ArtistFestival::getArtistId, Collectors.counting()));

        if (coAppearanceCount.isEmpty()) return List.of();

        // 공동 출연 많은 순 → 상위 limit 개 아티스트 ID 추출
        List<Long> topIds = coAppearanceCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, Artist> artistMap = artistRepository.findAllById(topIds).stream()
                .collect(Collectors.toMap(Artist::getId, a -> a));

        return topIds.stream()
                .map(artistMap::get)
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();
    }

    @Override
    public long getTotalCount() {
        return artistRepository.count();
    }
}
