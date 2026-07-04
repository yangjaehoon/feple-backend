package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;

import java.util.List;

public interface FestivalService {
    List<FestivalResponseDto> getAllFestivals(FestivalFilterCriteria criteria);
    FestivalDetailResponseDto getFestivalDetail(Long id);
    List<FestivalResponseDto> searchFestivals(String keyword);
    List<FestivalResponseDto> getLikedFestivals(Long userId);
}
