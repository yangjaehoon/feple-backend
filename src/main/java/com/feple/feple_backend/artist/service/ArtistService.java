package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final FileStorageService fileStorageService;

    private ArtistResponseDto toDto(Artist artist) {
        return ArtistResponseDto.from(artist, fileStorageService.buildUrl(artist.getProfileImageKey()));
    }

    public Long createArtist(ArtistRequestDto dto){
        Artist artist = Artist.builder()
                .name(dto.getName())
                .genre(dto.getGenre())
                .profileImageKey(dto.getProfileImageUrl())
                .build();

        return artistRepository.save(artist).getId();
    }

    public List<ArtistResponseDto> getAllArtists() {
        return artistRepository.findAll(PageRequest.of(0, 200,
                        Sort.by(Sort.Direction.DESC, "weeklyScore").and(Sort.by(Sort.Direction.ASC, "id"))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ArtistResponseDto> searchArtists(String keyword) {
        return artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ArtistResponseDto> getAdminArtistList(String sort, String keyword, ArtistGenre genre) {
        List<ArtistResponseDto> result;
        if (keyword != null && !keyword.isBlank()) {
            result = artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                    .map(this::toDto).toList();
        } else {
            result = switch (sort == null ? "" : sort) {
                case "name" -> artistRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                        .map(this::toDto).toList();
                case "name_desc" -> artistRepository.findAll(Sort.by(Sort.Direction.DESC, "name")).stream()
                        .map(this::toDto).toList();
                case "followers" -> artistRepository.findAll(Sort.by(Sort.Direction.DESC, "followerCount")).stream()
                        .map(this::toDto).toList();
                case "followers_asc" -> artistRepository.findAll(Sort.by(Sort.Direction.ASC, "followerCount")).stream()
                        .map(this::toDto).toList();
                default -> artistRepository.findAllByOrderByWeeklyScoreDescIdAsc().stream()
                        .map(this::toDto).toList();
            };
        }
        if (genre != null) {
            result = result.stream()
                    .filter(a -> genre.getDisplayName().equals(a.getGenre()))
                    .toList();
        }
        return result;
    }

    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        return toDto(artist);
    }

    public Page<ArtistResponseDto> getArtistsPage(int page, int size) {
        Page<Artist> result = artistRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return result.map(this::toDto);
    }

    public ArtistRequestDto getArtistForEdit(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        return ArtistRequestDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre())
                .profileImageUrl(fileStorageService.buildUrl(artist.getProfileImageKey()))
                .followerCount(artist.getFollowerCount())
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateArtist(Long id, ArtistRequestDto dto) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        String imageKey = dto.getProfileImageUrl() != null
                ? dto.getProfileImageUrl()
                : artist.getProfileImageKey();
        artist.update(dto.getName(), dto.getGenre(), imageKey);
    }

    public List<ArtistResponseDto> getTopArtists(int limit) {
        return artistRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "followerCount")))
                .getContent().stream().map(this::toDto).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateArtistPhoto(Long id, String imageKey) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        artist.update(artist.getName(), artist.getGenre(), imageKey);
    }

    public void deleteArtist(Long id) {
        artistRepository.deleteById(id);
    }
}
