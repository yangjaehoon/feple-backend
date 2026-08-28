package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.S3Properties;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3PresignService {
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public S3PresignedUrlResult presignPut(String objectKey, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(s3Properties.presignMinutes()))
                .putObjectRequest(putReq)
        );

        return new S3PresignedUrlResult(presigned.url().toString(), objectKey);
    }

    public String presignGetUrl(String objectKey) {
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofHours(s3Properties.getPresignHours()))
                .getObjectRequest(go -> go.bucket(s3Properties.bucket()).key(objectKey))
        );
        return presigned.url().toString();
    }
}
