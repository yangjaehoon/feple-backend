package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface FestivalService {
    Long createFestival(FestivalRequestDto dto);
    List<FestivalResponseDto> getAllFestivals(List<Genre> genres, List<Region> regions,
                                              List<AgeRestriction> ageRestrictions, boolean includeEnded);
    FestivalDetailResponseDto getFestivalDetail(Long id);
    FestivalResponseDto getFestival(Long id);
    void updateFestival(Long id, FestivalRequestDto dto);
    void deleteFestival(Long festivalId);
    List<FestivalResponseDto> searchFestivals(String keyword);
    Page<FestivalResponseDto> getFestivalsPage(int page, int size);
    Page<FestivalResponseDto> getFestivalsAdminPage(String keyword, int page, int size);
    String uploadPosterFile(MultipartFile file, LocalDate startDate) throws IOException;
    List<FestivalResponseDto> getLikedFestivals(Long userId);
    long countActiveFestivals(LocalDate today);
}
