package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.repository.ArtistRepository;
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

    public Long createArtist(ArtistRequestDto dto){
        Artist artist = Artist.builder()
                .name(dto.getName())
                .genre(dto.getGenre())
                .profileImageUrl(dto.getProfileImageUrl())
                .build();

        return artistRepository.save(artist).getId();
    }

    public List<ArtistResponseDto> getAllArtists() {
        return artistRepository.findAllByOrderByWeeklyScoreDescIdAsc().stream()
                .map(ArtistResponseDto::from)
                .toList();
    }

    public List<ArtistResponseDto> searchArtists(String keyword) {
        return artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                .map(ArtistResponseDto::from)
                .toList();
    }

    public List<ArtistResponseDto> getAdminArtistList(String sort, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword).stream()
                    .map(ArtistResponseDto::from).toList();
        }
        return switch (sort == null ? "" : sort) {
            case "name" -> artistRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                    .map(ArtistResponseDto::from).toList();
            case "name_desc" -> artistRepository.findAll(Sort.by(Sort.Direction.DESC, "name")).stream()
                    .map(ArtistResponseDto::from).toList();
            case "followers" -> artistRepository.findAll(Sort.by(Sort.Direction.DESC, "followerCount")).stream()
                    .map(ArtistResponseDto::from).toList();
            case "followers_asc" -> artistRepository.findAll(Sort.by(Sort.Direction.ASC, "followerCount")).stream()
                    .map(ArtistResponseDto::from).toList();
            default -> artistRepository.findAllByOrderByWeeklyScoreDescIdAsc().stream()
                    .map(ArtistResponseDto::from).toList();
        };
    }

    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));

        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre() != null ? artist.getGenre().getDisplayName() : null)
                .profileImageUrl(artist.getProfileImageUrl())
                .followerCount(artist.getFollowerCount())
                .build();
    }

    public Page<ArtistResponseDto> getArtistsPage(int page, int size) {
        Page<Artist> result = artistRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return result.map(ArtistResponseDto::from);
    }

    public ArtistRequestDto getArtistForEdit(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        return ArtistRequestDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre())
                .profileImageUrl(artist.getProfileImageUrl())
                .followerCount(artist.getFollowerCount())
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateArtist(Long id, ArtistRequestDto dto) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        String imageUrl = dto.getProfileImageUrl() != null
                ? dto.getProfileImageUrl()
                : artist.getProfileImageUrl();
        artist.update(dto.getName(), dto.getGenre(), imageUrl);
    }

    public List<ArtistResponseDto> getTopArtists(int limit) {
        return artistRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "followerCount")))
                .getContent().stream().map(ArtistResponseDto::from).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateArtistPhoto(Long id, String imageUrl) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));
        artist.update(artist.getName(), artist.getGenre(), imageUrl);
    }

    public void deleteArtist(Long id) {
        artistRepository.deleteById(id);
    }
}
