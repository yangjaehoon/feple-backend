package com.feple.feple_backend.booth.service;

import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.entity.Booth;
import com.feple.feple_backend.booth.repository.BoothRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    // imageKey는 사용자 입력 폼에 없는 값이라 DTO에 실어 받지 않고, 컨트롤러가
    // 업로드에 성공했을 때만 별도 파라미터로 전달한다 (mass assignment 방지)
    @Transactional
    public Long createBooth(Long festivalId, BoothRequestDto dto, String imageKey) {
        // 삭제된(휴지통) 페스티벌에는 새 부스를 만들 수 없다 — 다른 관리자가 방금
        // 삭제한 페스티벌의 부스 등록 폼이 아직 열려 있는 경우를 막는다
        Festival festival = EntityLoader.getOrThrow(
                festivalRepository::findByIdAndDeletedAtIsNull, festivalId, "페스티벌");
        // 이후 저장이 실패해 이 트랜잭션이 롤백되면 이미 업로드된 S3 파일도 함께 정리
        fileStorageService.deleteFileOnRollback(imageKey);
        Booth booth = Booth.builder()
                .festival(festival)
                .name(dto.getName())
                .boothType(dto.getBoothType())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .description(dto.getDescription())
                .imageKey(imageKey)
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
