package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
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
    // GET presigned URL TTL — 7일(최대값). CachedNetworkImage가 URL을 키로 캐시하므로
    // TTL이 짧으면 만료 후 재시도 시 403 발생
    @Value("${app.s3.get-presign-hours:168}") private long getPresignHours;

    public S3PresignedUrlResult presignPut(String objectKey, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .putObjectRequest(putReq)
        );

        return new S3PresignedUrlResult(presigned.url().toString(), objectKey);
    }

    public String presignGetUrl(String objectKey) {
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofHours(getPresignHours))
                .getObjectRequest(go -> go.bucket(bucket).key(objectKey))
        );
        return presigned.url().toString();
    }
}
