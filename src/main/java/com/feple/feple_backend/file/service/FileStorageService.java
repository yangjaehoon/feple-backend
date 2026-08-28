package com.feple.feple_backend.file.service;

import com.feple.feple_backend.file.CdnProperties;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.S3Properties;
import com.feple.feple_backend.global.exception.ExternalStorageException;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final int ARTIST_PROFILE_RESIZE_PX = 400;
    private static final int FESTIVAL_POSTER_RESIZE_PX = 720;
    private static final int BOOTH_IMAGE_RESIZE_PX = 300;

    // 기본(미설정) 프로필 이미지 — 이 경로가 저장돼 있으면 실제 업로드된 이미지가 아니므로 null 처리한다
    private static final String DEFAULT_PROFILE_IMAGE_PATH = "/img/feple_logo.png";

    private final S3Template s3Template;
    private final ImageResizeService imageResizeService;
    private final S3Properties s3Properties;
    private final CdnProperties cdnProperties;

    public String buildUrl(String key) {
        if (key == null) return null;
        if (key.startsWith("http")) return key;
        String cdnBaseUrl = cdnProperties.baseUrl();
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            String base = cdnBaseUrl.endsWith("/") ? cdnBaseUrl.substring(0, cdnBaseUrl.length() - 1) : cdnBaseUrl;
            return base + "/" + key;
        }
        return "https://" + s3Properties.bucket() + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    /** 프로필 이미지 key/URL을 화면에 표시할 절대 URL로 변환한다. 기본(미설정) 이미지 경로가
     * 저장돼 있으면 실제 업로드된 이미지가 아니므로 null을 반환한다. */
    public String resolveProfileImageUrl(String raw) {
        if (raw == null || raw.isBlank() || raw.contains(DEFAULT_PROFILE_IMAGE_PATH)) return null;
        return buildUrl(raw);
    }

    public String storeFestivalPoster(MultipartFile file, LocalDate festivalStartDate) throws IOException {
        imageResizeService.validateFile(file);
        String yearMonth = festivalStartDate == null ? ""
                : festivalStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String folder = yearMonth.isEmpty() ? S3PathConstants.POSTERS : S3PathConstants.POSTERS + "/" + yearMonth;
        return uploadResizedJpeg(file, folder + "/" + UUID.randomUUID() + ".jpg", FESTIVAL_POSTER_RESIZE_PX);
    }

    public String storeUserProfile(MultipartFile file, String nickname) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(nickname);
        return uploadResizedJpeg(file,
                S3PathConstants.USER_PROFILES + "/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_RESIZE_PX);
    }

    public String storeArtistProfile(MultipartFile file, String artistName) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(artistName);
        return uploadResizedJpeg(file,
                S3PathConstants.ARTISTS + "/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_RESIZE_PX);
    }

    public String storeAdminProfile(MultipartFile file, String username) throws IOException {
        imageResizeService.validateFile(file);
        String safeName = toSafeName(username);
        String key = uploadResizedJpeg(file,
                "admin-profiles/" + safeName + "/" + UUID.randomUUID() + ".jpg",
                ARTIST_PROFILE_RESIZE_PX);
        return buildUrl(key);
    }

    public String storeBoothImage(MultipartFile file) throws IOException {
        imageResizeService.validateFile(file);
        return uploadResizedJpeg(file,
                S3PathConstants.BOOTHS + "/" + UUID.randomUUID() + ".jpg",
                BOOTH_IMAGE_RESIZE_PX);
    }

    private String uploadResizedJpeg(MultipartFile file, String key, int maxPx) throws IOException {
        byte[] resized;
        try (InputStream in = file.getInputStream()) {
            resized = imageResizeService.resizeToJpeg(in, maxPx);
        }
        // S3 SDK는 업로드 실패를 unchecked(SdkException 등)로 던진다 — 외부 스토리지 장애이므로
        // 502로 매핑되도록 ExternalStorageException으로 변환한다(내부 500과 구분).
        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(s3Properties.bucket(), key, is);
            return key;
        } catch (RuntimeException e) {
            throw new ExternalStorageException("파일 저장소에 업로드하지 못했습니다.", e);
        }
    }

    private static String toSafeName(String name) {
        return (name == null || name.isBlank())
                ? "unknown"
                : name.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");
    }

    /**
     * S3 오브젝트 삭제. storeAdminProfile처럼 key 대신 buildUrl()이 적용된 완전한 URL을 저장해둔
     * 경우를 대비해, key가 URL 형태(http로 시작)면 CDN/S3 버킷 prefix를 제거해 실제 key를 추출한다.
     * 알 수 없는 외부 URL(prefix가 안 맞음)은 안전하게 삭제를 건너뛴다.
     */
    public void deleteFile(String key) {
        if (key == null || key.isBlank()) return;
        String objectKey = key.startsWith("http") ? stripToObjectKey(key) : key;
        if (objectKey == null) return;
        try {
            s3Template.deleteObject(s3Properties.bucket(), objectKey);
        } catch (Exception e) {
            // S3 DeleteObject는 idempotent해서 key가 이미 없어도 예외 없이 성공한다.
            // 즉 여기 잡히는 예외는 권한 오류·네트워크 문제 등 진짜 삭제 실패이므로 반드시 로그를 남긴다.
            log.warn("[FileStorage] S3 파일 삭제 실패 key={}", objectKey, e);
        }
    }

    private String stripToObjectKey(String url) {
        String defaultS3Prefix = "https://" + s3Properties.bucket() + ".s3.ap-northeast-2.amazonaws.com/";
        String cdnBaseUrl = cdnProperties.baseUrl();
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            String base = cdnBaseUrl.endsWith("/") ? cdnBaseUrl : cdnBaseUrl + "/";
            if (url.startsWith(base)) return url.substring(base.length());
        }
        if (url.startsWith(defaultS3Prefix)) return url.substring(defaultS3Prefix.length());
        return null;
    }

    /**
     * 활성 트랜잭션이 있으면 커밋 후 삭제, 없으면 즉시 삭제.
     * DB 커넥션을 S3 I/O 동안 점유하지 않으면서 롤백 시 파일을 보존한다.
     */
    public void deleteFileAfterCommit(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFile(key);
                }
            });
        } else {
            deleteFile(key);
        }
    }

    /**
     * 트랜잭션 안에서 새로 업로드한 파일을 등록해두면, 이후 DB 저장 실패로 트랜잭션이 롤백될 때만
     * 정리한다(커밋되면 그대로 유지). 활성 트랜잭션이 없으면 롤백 개념이 없으므로 아무 것도 하지 않는다.
     */
    public void deleteFileOnRollback(String key) {
        if (key == null || key.isBlank()) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteFile(key);
                }
            }
        });
    }

}
