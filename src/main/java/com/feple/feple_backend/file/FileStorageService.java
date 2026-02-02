package com.feple.feple_backend.file;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public String storeFile(MultipartFile file, LocalDate festivalStartDate) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");

        String yearMonth = "";
        if (festivalStartDate != null) {
            yearMonth = festivalStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }

        String uuid = UUID.randomUUID().toString();
        String savedName = yearMonth.isEmpty() ? uuid + ext : yearMonth + "_" + uuid + ext;

        String key = yearMonth.isEmpty()
                ? "posters/" + savedName
                : "posters/" + yearMonth + "/" + savedName;

        S3Resource result;
        try (InputStream is = file.getInputStream()) {
            result = s3Template.upload(bucket, key, is);
        }

        return result.getURL().toString();
    }
}
