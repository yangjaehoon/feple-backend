package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

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
    private final SongRepository songRepository;

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
        return artistRepository.findByNameOrNameEnContainingIgnoreCase(keyword).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getAdminArtistList(String sort, String keyword, ArtistGenre genre) {
        Map<Long, Integer> songCountMap = songRepository.countGroupedByArtist().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        Stream<Artist> stream;
        if (keyword != null && !keyword.isBlank()) {
            stream = artistRepository.findByNameOrNameEnContainingIgnoreCase(keyword).stream();
        } else if ("songs".equals(sort) || "songs_asc".equals(sort)) {
            stream = artistRepository.findAll().stream();
        } else {
            stream = SORT_STRATEGIES.getOrDefault(sort, ArtistRepository::findAllByOrderByWeeklyScoreDescIdAsc)
                                     .apply(artistRepository).stream();
        }
        if (genre != null) {
            stream = stream.filter(a -> genre == a.getGenre());
        }
        List<ArtistResponseDto> result = stream
                .map(a -> ArtistResponseDto.from(a,
                        fileStorageService.buildUrl(a.getProfileImageKey()),
                        songCountMap.getOrDefault(a.getId(), 0)))
                .collect(Collectors.toCollection(java.util.ArrayList::new));

        if ("songs".equals(sort)) {
            result.sort(Comparator.comparingInt(ArtistResponseDto::getSongCount).reversed());
        } else if ("songs_asc".equals(sort)) {
            result.sort(Comparator.comparingInt(ArtistResponseDto::getSongCount));
        }
        return result;
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
        artist.update(dto.getName(), dto.getNameEn(), dto.getGenre());
        if (dto.getProfileImageKey() != null) {
            String oldKey = artist.getProfileImageKey();
            artist.updateProfileImage(dto.getProfileImageKey());
            if (oldKey != null) fileStorageService.deleteFile(oldKey);
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
        artist.updateProfileImage(imageKey);
        if (oldKey != null) fileStorageService.deleteFile(oldKey);
    }

    @Override
    @Transactional
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
    public void deleteArtist(Long id) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, id, "아티스트");
        cascadeDeleteService.delete(artist);
    }
}
