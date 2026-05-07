package com.feple.feple_backend.artist.photo.scheduler;

import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistPhotoLikeResetScheduler {

    private final ArtistGalleryPhotoRepository photoRepository;
    private final ArtistGalleryPhotoLikeRepository photoLikeRepository;

    /** 매주 월요일 자정에 사진 좋아요 집계를 초기화한다. */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetWeeklyLikes() {
        log.info("[ArtistPhotoLikeResetScheduler] 주간 좋아요 초기화 시작");
        photoLikeRepository.deleteAll();
        photoRepository.resetAllLikeCounts();
        log.info("[ArtistPhotoLikeResetScheduler] 주간 좋아요 초기화 완료");
    }
}
