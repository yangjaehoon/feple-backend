package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.festival.Artist;
import com.feple.feple_backend.domain.festival.Festival;
import com.feple.feple_backend.dto.festival.FestivalRequestDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        if(dto.getArtists() != null) {
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
}
