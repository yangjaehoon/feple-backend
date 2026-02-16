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
        return artistRepository.findAll().stream()
                .map(ArtistResponseDto::from)
                .toList();
    }

    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));

        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre())
                .profileImageUrl(artist.getProfileImageUrl())
                .followerCount(artist.getFollowerCount())
                .build();
    }

    public Page<ArtistResponseDto> getArtistsPage(int page, int size) {
        Page<Artist> result = artistRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return result.map(ArtistResponseDto::from);
    }

    public void deleteArtist(Long id) {
        artistRepository.deleteById(id);
    }
}
