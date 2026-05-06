package com.feple.feple_backend.artist.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignService {
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}") private String bucket;
    @Value("${app.s3.presign-minutes:10}") private long presignMinutes;

    public PresignResult presignPut(String objectKey, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .putObjectRequest(putReq)
        );

        return new PresignResult(presigned.url().toString(), objectKey);
    }

    public record PresignResult(String uploadUrl, String objectKey) {}

    public String presignGetUrl(String objectKey) {
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(go -> go.bucket(bucket).key(objectKey))
        );
        return presigned.url().toString();
    }
}