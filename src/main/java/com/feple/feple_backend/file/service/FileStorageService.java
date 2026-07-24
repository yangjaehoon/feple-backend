package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.S3PathConstants;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final int ARTIST_PROFILE_RESIZE_PX = 400;
    private static final int FESTIVAL_POSTER_RESIZE_PX = 720;
    private static final int BOOTH_IMAGE_RESIZE_PX = 300;

    private final S3Template s3Template;
    private final ImageResizeService imageResizeService;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.cdn.base-url:}")
    private String cdnBaseUrl;

    public String buildUrl(String key) {
        if (key == null) return null;
        if (key.startsWith("http")) return key;
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            String base = cdnBaseUrl.endsWith("/") ? cdnBaseUrl.substring(0, cdnBaseUrl.length() - 1) : cdnBaseUrl;
            return base + "/" + key;
        }
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    public String storeFestivalPoster(MultipartFile file, LocalDate festivalStartDate) throws IOException {
        imageResizeService.validateFile(file);
        String yearMonth = festivalStartDate == null ? ""
                : festivalStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String folder = yearMonth.isEmpty() ? S3PathConstants.POSTERS : S3PathConstants.POSTERS + "/" + yearMonth;
        return uploadResizedJpeg(file, folder + "/" + UUID.randomUUID() + ".jpg", FESTIVAL_POSTER_RESIZE_PX);
    }

    public String storeUserProfile(MultipartFile file, String nickname) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(nickname);
        return uploadResizedJpeg(file,
                S3PathConstants.USER_PROFILES + "/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_RESIZE_PX);
    }

    public String storeArtistProfile(MultipartFile file, String artistName) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(artistName);
        return uploadResizedJpeg(file,
                S3PathConstants.ARTISTS + "/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_RESIZE_PX);
    }

    public String storeAdminProfile(MultipartFile file, String username) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(username);
        String key = uploadResizedJpeg(file,
                "admin-profiles/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_RESIZE_PX);
        return buildUrl(key);
    }

    public String storeBoothImage(MultipartFile file) throws IOException {
        imageResizeService.validateFile(file);
        return uploadResizedJpeg(file,
                S3PathConstants.BOOTHS + "/" + UUID.randomUUID() + ".jpg",
                BOOTH_IMAGE_RESIZE_PX);
    }

    private String uploadResizedJpeg(MultipartFile file, String key, int maxPx) throws IOException {
        byte[] resized;
        try (InputStream in = file.getInputStream()) {
            resized = imageResizeService.resizeToJpeg(in, maxPx);
        }
        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    private static String toSafeName(String name) {
        return (name == null || name.isBlank())
                ? "unknown"
                : name.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");
    }

    /**
     * S3 오브젝트 삭제. storeAdminProfile처럼 key 대신 buildUrl()이 적용된 완전한 URL을 저장해둔
     * 경우를 대비해, key가 URL 형태(http로 시작)면 CDN/S3 버킷 prefix를 제거해 실제 key를 추출한다.
     * 알 수 없는 외부 URL(prefix가 안 맞음)은 안전하게 삭제를 건너뛴다.
     */
    public void deleteFile(String key) {
        if (key == null || key.isBlank()) return;
        String objectKey = key.startsWith("http") ? stripToObjectKey(key) : key;
        if (objectKey == null) return;
        try {
            s3Template.deleteObject(bucket, objectKey);
        } catch (Exception e) {
            // S3 DeleteObject는 idempotent해서 key가 이미 없어도 예외 없이 성공한다.
            // 즉 여기 잡히는 예외는 권한 오류·네트워크 문제 등 진짜 삭제 실패이므로 반드시 로그를 남긴다.
            log.warn("[FileStorage] S3 파일 삭제 실패 key={}", objectKey, e);
        }
    }

    private String stripToObjectKey(String url) {
        String defaultS3Prefix = "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/";
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            String base = cdnBaseUrl.endsWith("/") ? cdnBaseUrl : cdnBaseUrl + "/";
            if (url.startsWith(base)) return url.substring(base.length());
        }
        if (url.startsWith(defaultS3Prefix)) return url.substring(defaultS3Prefix.length());
        return null;
    }

    /**
     * 활성 트랜잭션이 있으면 커밋 후 삭제, 없으면 즉시 삭제.
     * DB 커넥션을 S3 I/O 동안 점유하지 않으면서 롤백 시 파일을 보존한다.
     */
    public void deleteFileAfterCommit(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFile(key);
                }
            });
        } else {
            deleteFile(key);
        }
    }

}
