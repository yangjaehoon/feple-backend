package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FestivalService {
    List<FestivalResponseDto> getAllFestivals(FestivalFilterCriteria criteria);
    Page<FestivalResponseDto> getFestivalsPage(FestivalFilterCriteria criteria, Pageable pageable);
    FestivalDetailResponseDto getFestivalDetail(Long id);
    List<FestivalResponseDto> searchFestivals(String keyword);
    List<FestivalResponseDto> getLikedFestivals(Long userId);
}
