package com.feple.feple_backend.booth.service;

import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.entity.Booth;
import com.feple.feple_backend.booth.repository.BoothRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoothService {

    private final BoothRepository boothRepository;
    private final FestivalRepository festivalRepository;

    @Transactional(readOnly = true)
    public List<BoothResponseDto> getBooths(Long festivalId) {
        return boothRepository.findByFestivalId(festivalId)
                .stream()
                .map(BoothResponseDto::from)
                .toList();
    }

    @Transactional
    public Long createBooth(Long festivalId, BoothRequestDto dto) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 페스티벌입니다."));
        Booth booth = Booth.builder()
                .festival(festival)
                .name(dto.getName())
                .boothType(dto.getBoothType())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .description(dto.getDescription())
                .build();
        return boothRepository.save(booth).getId();
    }

    @Transactional
    public void deleteBooth(Long boothId) {
        boothRepository.deleteById(boothId);
    }
}
