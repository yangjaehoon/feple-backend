package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.festival.domain.Festival;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;

    @Transactional
    public void createFestival(FestivalRequestDto dto) {
        Festival festival = Festival.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .location(dto.getLocation())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .posterUrl(dto.getPosterUrl())
                .build();

        if (dto.getArtists() != null) {
            dto.getArtists().forEach(artistDto -> {
                Artist artist = Artist.builder()
                        .name(artistDto.getName())
                        .genre(artistDto.getGenre())
                        .profileImageUrl(artistDto.getProfileImageUrl())
                        .festival(festival)
                        .build();
                festival.getArtists().add(artist);
            });
        }
        festivalRepository.save(festival);
    }

    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getAllFestivals() {
        return festivalRepository.findAll().stream()
                .map(festival -> FestivalResponseDto.builder()
                        .id(festival.getId())
                        .title(festival.getTitle())
                        .description(festival.getDescription())
                        .location(festival.getLocation())
                        .startDate(festival.getStartDate())
                        .endDate(festival.getEndDate())
                        .posterUrl(festival.getPosterUrl())
                        .artists(
                                festival.getArtists().stream()
                                        .map(artist -> ArtistResponseDto.builder()
                                                .id(artist.getId())
                                                .name(artist.getName())
                                                .genre(artist.getGenre())
                                                .profileImageUrl(artist.getProfileImageUrl())
                                                .build())
                                        .toList()
                        )
                        .build()
                )
                .toList();
    }
}
