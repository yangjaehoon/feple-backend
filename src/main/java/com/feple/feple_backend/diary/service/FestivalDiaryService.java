package com.feple.feple_backend.diary.service;

import com.feple.feple_backend.diary.dto.CreateDiaryRequestDto;
import com.feple.feple_backend.diary.dto.FestivalDiaryResponseDto;
import com.feple.feple_backend.diary.dto.UpdateDiaryRequestDto;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import java.util.List;
import org.springframework.data.domain.Page;

public interface FestivalDiaryService {

    S3PresignedUrlResult generateUploadUrl(Long userId, String extension, String contentType);

    FestivalDiaryResponseDto create(Long userId, Long festivalId, CreateDiaryRequestDto req);

    List<FestivalDiaryResponseDto> getMyDiaries(Long userId, Long festivalId);

    FestivalDiaryResponseDto getDiary(Long viewerId, Long diaryId);

    FestivalDiaryResponseDto update(Long userId, Long diaryId, UpdateDiaryRequestDto req);

    void delete(Long userId, Long diaryId);

    Page<FestivalDiaryResponseDto> getPublicFeed(Long festivalId, int page, Long viewerId);

    void removeAllByUser(Long userId);
}
