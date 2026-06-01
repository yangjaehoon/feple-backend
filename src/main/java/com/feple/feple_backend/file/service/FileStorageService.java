package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.S3Keys;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final int ARTIST_PROFILE_MAX_PX = 400;
    private static final int FESTIVAL_POSTER_MAX_PX = 720;
    private static final int BOOTH_IMAGE_MAX_PX = 300;

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
        String folder = yearMonth.isEmpty() ? S3Keys.POSTERS : S3Keys.POSTERS + "/" + yearMonth;
        return uploadResizedJpeg(file, folder + "/" + UUID.randomUUID() + ".jpg", FESTIVAL_POSTER_MAX_PX);
    }

    public String storeUserProfile(MultipartFile file, String nickname) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(nickname);
        return uploadResizedJpeg(file,
                S3Keys.USER_PROFILES + "/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_MAX_PX);
    }

    public String storeArtistProfile(MultipartFile file, String artistName) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(artistName);
        return uploadResizedJpeg(file,
                S3Keys.ARTISTS + "/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_MAX_PX);
    }

    public String storeAdminProfile(MultipartFile file, String username) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(username);
        String key = uploadResizedJpeg(file,
                "admin-profiles/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_MAX_PX);
        return buildUrl(key);
    }

    public String storeBoothImage(MultipartFile file) throws IOException {
        imageResizeService.validateFile(file);
        return uploadResizedJpeg(file,
                S3Keys.BOOTHS + "/" + UUID.randomUUID() + ".jpg",
                BOOTH_IMAGE_MAX_PX);
    }

    private String uploadResizedJpeg(MultipartFile file, String key, int maxPx) throws IOException {
        byte[] resized = imageResizeService.resizeToJpeg(file.getInputStream(), maxPx);
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

    /** S3 오브젝트 삭제 (회원 탈퇴 시 프로필 이미지 정리용) */
    public void deleteFile(String key) {
        if (key == null || key.isBlank() || key.startsWith("http")) return;
        try {
            s3Template.deleteObject(bucket, key);
        } catch (Exception ignored) {
            // 이미 삭제되었거나 존재하지 않는 경우 무시
        }
    }

}
