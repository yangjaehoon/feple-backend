package com.feple.feple_backend.booth.service;

import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.entity.Booth;
import com.feple.feple_backend.booth.repository.BoothRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BoothService {

    private final BoothRepository boothRepository;
    private final FestivalRepository festivalRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<BoothResponseDto> getBooths(Long festivalId) {
        return boothRepository.findByFestivalId(festivalId)
                .stream()
                .map(b -> BoothResponseDto.from(b, fileStorageService.buildUrl(b.getImageKey())))
                .toList();
    }

    @Transactional
    public Long createBooth(Long festivalId, BoothRequestDto dto) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 페스티벌입니다."));
        Booth booth = Booth.builder()
                .festival(festival)
                .name(dto.getName())
                .boothType(dto.getBoothType())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .description(dto.getDescription())
                .imageKey(dto.getImageKey())
                .build();
        return boothRepository.save(booth).getId();
    }

    @Transactional
    public void deleteBooth(Long boothId) {
        boothRepository.deleteById(boothId);
    }

    public String uploadBoothImage(MultipartFile file) throws IOException {
        return fileStorageService.storeBoothImage(file);
    }
}
