package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.domain.Festival;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;

    @Transactional
    public Long createFestival(FestivalRequestDto dto) {
        Festival festival = Festival.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .location(dto.getLocation())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .posterUrl(dto.getPosterUrl())
                .build();

        Festival saved = festivalRepository.save(festival);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getAllFestivals() {
        return festivalRepository.findAllByOrderByStartDateDesc()
                .stream()
                .map(FestivalResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FestivalDetailResponseDto getFestivalDetail(Long id) {
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + id));

        return FestivalDetailResponseDto.from(festival);
    }

    @Transactional(readOnly = true)
    public FestivalResponseDto getFestival(Long id) {
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + id));

        return FestivalResponseDto.from(festival);
    }

    @Transactional
    public void updateFestival(Long id, FestivalRequestDto dto) {
        Festival festival = festivalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + id));

        festival.setTitle(dto.getTitle());
        festival.setDescription(dto.getDescription());
        festival.setLocation(dto.getLocation());
        festival.setStartDate(dto.getStartDate());
        festival.setEndDate(dto.getEndDate());
        festival.setPosterUrl(dto.getPosterUrl());
    }

    @Transactional
    public void deleteFestival(Long festivalId) {

        if (!festivalRepository.existsById(festivalId)) {
            throw new IllegalArgumentException("존재하지 않는 페스티벌입니다. id=" + festivalId);
        }

        artistFestivalRepository.deleteByFestivalId(festivalId);
        festivalRepository.deleteById(festivalId);
    }

    @Transactional
    public Page<FestivalResponseDto> getFestivalsPage(int page, int size) {
        Page<Festival> result = festivalRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate")));
        return result.map(FestivalResponseDto::from);
    }

}
