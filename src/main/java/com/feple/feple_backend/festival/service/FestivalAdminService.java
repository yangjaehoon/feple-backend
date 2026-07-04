package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface FestivalAdminService {
    Long createFestival(FestivalRequestDto dto);
    List<FestivalResponseDto> getAllFestivalsForAdmin();
    List<FestivalResponseDto> getAllActiveFestivalsForAdmin();
    FestivalResponseDto getFestival(Long id);
    void updateFestival(Long id, FestivalRequestDto dto);
    void deleteFestival(Long festivalId);
    Page<FestivalResponseDto> getFestivalsAdminPage(String keyword, int page, int size);
    String uploadPosterFile(MultipartFile file, LocalDate startDate) throws IOException;
    long getTotalCount();
}
