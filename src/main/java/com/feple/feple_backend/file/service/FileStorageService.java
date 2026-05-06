package com.feple.feple_backend.file.service;

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

    private final S3Template s3Template;
    private final ImageResizeService imageResizeService;

    @Value("${app.s3.bucket}")
    private String bucket;

    public String buildUrl(String key) {
        if (key == null)
            return null;
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    public String storeFestivalPoster(MultipartFile file, LocalDate festivalStartDate) throws IOException {
        imageResizeService.validateFile(file);

        String yearMonth = festivalStartDate == null ? ""
                : festivalStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String folder = yearMonth.isEmpty() ? "posters" : "posters/" + yearMonth;

        byte[] resized = imageResizeService.resizeToJpeg(file.getInputStream(), FESTIVAL_POSTER_MAX_PX);
        String key = folder + "/" + UUID.randomUUID() + ".jpg";

        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    public String storeUserProfile(MultipartFile file, String nickname) throws IOException {
        imageResizeService.validateFile(file);

        String safeName = (nickname == null || nickname.isBlank())
                ? "unknown"
                : nickname.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");

        byte[] resized = imageResizeService.resizeToJpeg(file.getInputStream(), ARTIST_PROFILE_MAX_PX);
        String key = "user-profiles/" + safeName + "/" + UUID.randomUUID() + ".jpg";

        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    public String storeArtistProfile(MultipartFile file, String artistName) throws IOException {
        imageResizeService.validateFile(file);

        String safeName = (artistName == null || artistName.isBlank())
                ? "unknown"
                : artistName.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");

        byte[] resized = imageResizeService.resizeToJpeg(file.getInputStream(), ARTIST_PROFILE_MAX_PX);
        String key = "artists/" + safeName + "/" + UUID.randomUUID() + ".jpg";

        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    public String storeBoothImage(MultipartFile file) throws IOException {
        imageResizeService.validateFile(file);
        byte[] resized = imageResizeService.resizeToJpeg(file.getInputStream(), 300);
        String key = "booths/" + UUID.randomUUID() + ".jpg";
        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
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
