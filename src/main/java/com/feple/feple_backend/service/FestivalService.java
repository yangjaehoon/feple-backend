package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.festival.Artist;
import com.feple.feple_backend.domain.festival.Festival;
import com.feple.feple_backend.dto.artist.ArtistResponseDto;
import com.feple.feple_backend.dto.festival.FestivalRequestDto;
import com.feple.feple_backend.dto.festival.FestivalResponseDto;
import com.feple.feple_backend.repository.FestivalRepository;
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
                        .genre(artistDto.getName())
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
                        .startDate(festival.getEndDate())
                        .posterUrl(festival.getPosterUrl())
                        .artists(
                                festival.getArtists().stream()
                                        .map(artist -> ArtistResponseDto.builder()
                                                .id(artist.getId())
                                                .name(artist.getName())
                                                .genre(artist.getName())
                                                .profileImageUrl(artist.getProfileImageUrl())
                                                .build())
                                        .toList()
                        )
                        .build()
                )
                .toList();
    }
}
