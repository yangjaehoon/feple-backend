package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    /** 정렬 전략 맵 — 새 정렬 옵션 추가 시 이곳에만 추가 (Strategy Pattern) */
    private static final Map<String, Function<ArtistRepository, List<Artist>>> SORT_STRATEGIES = Map.of(
        "name",          repo -> repo.findAll(Sort.by(Sort.Direction.ASC,  "name")),
        "name_desc",     repo -> repo.findAll(Sort.by(Sort.Direction.DESC, "name")),
        "followers",     repo -> repo.findAll(Sort.by(Sort.Direction.DESC, "followerCount")),
        "followers_asc", repo -> repo.findAll(Sort.by(Sort.Direction.ASC,  "followerCount"))
    );

    private final ArtistRepository artistRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final FileStorageService fileStorageService;
    private final ArtistCascadeDeleteService cascadeDeleteService;

    private ArtistResponseDto toDto(Artist artist) {
        return ArtistResponseDto.from(artist, fileStorageService.buildUrl(artist.getProfileImageKey()));
    }

    @Override
    @Transactional
    public Long createArtist(ArtistRequestDto dto) {
        Artist artist = Artist.builder()
                .name(dto.getName())
                .nameEn(dto.getNameEn())
                .genre(dto.getGenre())
                .profileImageKey(dto.getProfileImageKey())
                .build();
        return artistRepository.save(artist).getId();
    }

    @Override
    @Transactional(readOnly = true)
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
        return artistFollowRepository.findByUserId(userId).stream()
                .map(follow -> toDto(follow.getArtist()))
                .toList();
    }

    @Override
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
        return artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getAdminArtistList(String sort, String keyword, ArtistGenre genre) {
        Stream<ArtistResponseDto> stream;
        if (keyword != null && !keyword.isBlank()) {
            stream = artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream().map(this::toDto);
        } else {
            stream = SORT_STRATEGIES.getOrDefault(sort, ArtistRepository::findAllByOrderByWeeklyScoreDescIdAsc)
                                     .apply(artistRepository).stream().map(this::toDto);
        }
        if (genre != null) {
            return stream.filter(a -> genre.getDisplayName().equals(a.getGenre())).toList();
        }
        return stream.toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        return toDto(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArtistResponseDto> getArtistsPage(int page, int size) {
        Page<Artist> result = artistRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return result.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistRequestDto getArtistForEdit(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        return ArtistRequestDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .nameEn(artist.getNameEn())
                .genre(artist.getGenre())
                .profileImageKey(fileStorageService.buildUrl(artist.getProfileImageKey()))
                .followerCount(artist.getFollowerCount())
                .build();
    }

    @Override
    @Transactional
    public void updateArtist(Long id, ArtistRequestDto dto) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        String oldKey = artist.getProfileImageKey();
        String imageKey = dto.getProfileImageKey() != null ? dto.getProfileImageKey() : oldKey;
        artist.update(dto.getName(), dto.getNameEn(), dto.getGenre(), imageKey);
        if (dto.getProfileImageKey() != null && !dto.getProfileImageKey().equals(oldKey)) {
            fileStorageService.deleteFile(oldKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getTopArtists(int limit) {
        return artistRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "followerCount")))
                .getContent().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void updateArtistPhoto(Long id, String imageKey) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        String oldKey = artist.getProfileImageKey();
        artist.update(artist.getName(), artist.getNameEn(), artist.getGenre(), imageKey);
        if (imageKey != null && !imageKey.equals(oldKey)) {
            fileStorageService.deleteFile(oldKey);
        }
    }

    @Override
    @Transactional
    public void batchUpdateNameEn(List<Long> ids, List<String> nameEns) {
        for (int i = 0; i < ids.size(); i++) {
            Artist artist = artistRepository.findById(ids.get(i)).orElse(null);
            if (artist != null) {
                String nameEn = (i < nameEns.size()) ? nameEns.get(i).trim() : "";
                artist.update(artist.getName(), nameEn.isEmpty() ? null : nameEn,
                        artist.getGenre(), artist.getProfileImageKey());
            }
        }
    }

    @Override
    @Transactional
    public void deleteArtist(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        cascadeDeleteService.delete(artist);
    }
}
