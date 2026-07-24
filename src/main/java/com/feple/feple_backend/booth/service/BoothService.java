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

import com.feple.feple_backend.global.EntityLoader;

import java.io.IOException;
import java.util.List;

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
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
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
    public void deleteBooth(Long festivalId, Long boothId) {
        Booth booth = EntityLoader.getOrThrow(boothRepository::findById, boothId, "부스");
        EntityLoader.requireBelongsToFestival(festivalId, booth.getFestivalId(), "부스가");
        boothRepository.delete(booth);
        fileStorageService.deleteFileAfterCommit(booth.getImageKey());
    }

    public String uploadBoothImage(MultipartFile file) throws IOException {
        return fileStorageService.storeBoothImage(file);
    }

    @Transactional
    public void removeAllByFestival(Long festivalId) {
        // 벌크 DELETE 쿼리라 삭제될 row의 imageKey를 미리 읽어둬야 S3 정리가 가능하다
        boothRepository.findByFestivalId(festivalId)
                .forEach(booth -> fileStorageService.deleteFileAfterCommit(booth.getImageKey()));
        boothRepository.deleteByFestivalId(festivalId);
    }
}
