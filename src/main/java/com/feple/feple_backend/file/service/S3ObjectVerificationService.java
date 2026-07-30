package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.ImageUploadPolicy;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** presigned URL로 클라이언트가 실제 업로드했는지, 허용된 이미지 타입·크기인지 S3에서 직접 검증 */
@Slf4j
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
        if (head.contentLength() != null && head.contentLength() > ImageUploadPolicy.MAX_PRESIGNED_UPLOAD_BYTES) {
            deleteOversizedObject(objectKey);
            throw new IllegalArgumentException("파일 크기가 너무 큽니다.");
        }
    }

    // presigned PUT URL 자체는 Content-Length를 제한할 수 없으므로 업로드 완료 후 삭제
    private void deleteOversizedObject(String objectKey) {
        try {
            s3Client.deleteObject(r -> r.bucket(bucket).key(objectKey));
        } catch (Exception e) {
            log.warn("[S3Verification] 초과 용량 오브젝트 삭제 실패 key={}", objectKey, e);
        }
    }
}
