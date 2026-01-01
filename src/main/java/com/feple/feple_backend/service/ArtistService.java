package com.feple.feple_backend.service;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository artistRepository;

    public List<ArtistResponseDto> getAllArtists() {
        return artistRepository.findAll().stream()
                .map(artist -> ArtistResponseDto.builder()
                        .id(artist.getId())
                        .name(artist.getName())
                        .genre(artist.getGenre())
                        .profileImageUrl(artist.getProfileImageUrl())
                        .likeCount(artist.getLikeCount())
                        .build())
                .collect(Collectors.toList());
    }

    public ArtistResponseDto getArtistById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아티스트가 존재하지 않습니다. id=" + id));

        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre())
                .profileImageUrl(artist.getProfileImageUrl())
                .likeCount(artist.getLikeCount())
                .build();
    }
}
