package com.feple.feple_backend.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Set;

/** presigned URL로 클라이언트가 실제 업로드했는지, 허용된 이미지 타입인지 S3에서 직접 검증 */
@Service
@RequiredArgsConstructor
public class S3ObjectVerificationService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    public void verifyImageObject(String objectKey) {
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(r -> r.bucket(bucket).key(objectKey));
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("업로드된 파일을 찾을 수 없습니다.");
        }
        String ct = head.contentType();
        String baseType = (ct == null) ? "" : ct.split(";")[0].trim();
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(baseType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. 이미지 파일만 등록할 수 있습니다.");
        }
    }
}
