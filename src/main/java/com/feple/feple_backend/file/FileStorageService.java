package com.feple.feple_backend.file;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String storeFestivalPoster(MultipartFile file, LocalDate festivalStartDate) throws IOException {
        String yearMonth = festivalStartDate == null ? ""
                : festivalStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        String folder = yearMonth.isEmpty() ? "posters" : "posters/" + yearMonth;
        return storeFile(file, folder);
    }

    public String storeArtistProfile(MultipartFile file, String artistName) throws IOException {
        String safeName = (artistName == null || artistName.isBlank())
                ? "unknown"
                : artistName.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");

        return storeFile(file, "artists/" + safeName);
    }

    private String storeFile(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty())
            throw new IllegalArgumentException("파일이 비어있습니다.");

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
        }

        String savedName = UUID.randomUUID() + ext;
        String key = folder + "/" + savedName;

        try (InputStream is = file.getInputStream()) {
            @SuppressWarnings("null")
            S3Resource result = s3Template.upload(bucket, key, is);
            return result.getURL().toString();
        }
    }
}
