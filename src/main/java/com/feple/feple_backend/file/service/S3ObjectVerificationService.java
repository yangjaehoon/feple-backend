package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.ImageUploadPolicy;
import com.feple.feple_backend.file.S3Properties;
import com.feple.feple_backend.global.exception.ExternalStorageException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
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
    private final S3Properties s3Properties;

    public void verifyImageObject(String objectKey) {
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(r -> r.bucket(s3Properties.bucket()).key(objectKey));
        } catch (NoSuchKeyException e) {
            throw new InvalidRequestException("업로드된 파일을 찾을 수 없습니다.");
        } catch (SdkException e) {
            // 권한 회수·네트워크 오류·S3 5xx 등은 클라이언트 잘못이 아니라 외부 스토리지 장애다.
            // 변환하지 않으면 GlobalExceptionHandler의 catch-all로 떨어져 502여야 할 응답이
            // 500 + log.error(Sentry)로 기록돼, 의존성 장애가 내부 결함으로 위장된다.
            // (FileStorageService.uploadResizedJpeg와 동일한 규칙)
            throw new ExternalStorageException("파일 저장소 조회에 실패했습니다.", e);
        }
        String ct = head.contentType();
        String baseType = (ct == null) ? "" : ct.split(";")[0].trim();
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(baseType)) {
            throw new InvalidRequestException("허용되지 않는 파일 형식입니다. 이미지 파일만 등록할 수 있습니다.");
        }
        if (head.contentLength() != null && head.contentLength() > ImageUploadPolicy.MAX_IMAGE_UPLOAD_BYTES) {
            deleteOversizedObject(objectKey);
            throw new InvalidRequestException("파일 크기가 너무 큽니다.");
        }
    }

    // presigned PUT URL 자체는 Content-Length를 제한할 수 없으므로 업로드 완료 후 삭제
    private void deleteOversizedObject(String objectKey) {
        try {
            s3Client.deleteObject(r -> r.bucket(s3Properties.bucket()).key(objectKey));
        } catch (Exception e) {
            log.warn("[S3Verification] 초과 용량 오브젝트 삭제 실패 key={}", objectKey, e);
        }
    }
}
