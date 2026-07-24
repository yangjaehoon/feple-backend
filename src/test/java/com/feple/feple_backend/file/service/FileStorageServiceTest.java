package com.feple.feple_backend.file.service;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock S3Template s3Template;
    @Mock ImageResizeService imageResizeService;

    @InjectMocks FileStorageService service;

    @AfterEach
    void clearTransactionSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void setBucketAndCdn(String bucket, String cdnBaseUrl) {
        ReflectionTestUtils.setField(service, "bucket", bucket);
        ReflectionTestUtils.setField(service, "cdnBaseUrl", cdnBaseUrl);
    }

    // ── buildUrl ──────────────────────────────────────────────────────────

    @Test
    void buildUrl_key가_null이면_null_반환() {
        assertThat(service.buildUrl(null)).isNull();
    }

    @Test
    void buildUrl_이미_http_URL이면_그대로_반환() {
        assertThat(service.buildUrl("https://example.com/a.jpg")).isEqualTo("https://example.com/a.jpg");
    }

    @Test
    void buildUrl_CDN_설정있으면_CDN_URL_반환() {
        setBucketAndCdn("my-bucket", "https://cdn.example.com/");

        assertThat(service.buildUrl("posters/a.jpg")).isEqualTo("https://cdn.example.com/posters/a.jpg");
    }

    @Test
    void buildUrl_CDN_없으면_S3_직접_URL_반환() {
        setBucketAndCdn("my-bucket", "");

        assertThat(service.buildUrl("posters/a.jpg"))
                .isEqualTo("https://my-bucket.s3.ap-northeast-2.amazonaws.com/posters/a.jpg");
    }

    // ── storeFestivalPoster ───────────────────────────────────────────────

    @Test
    void 페스티벌_포스터_저장_시작일_있으면_연월_폴더_포함() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "poster.jpg", "image/jpeg", new byte[]{1});
        given(imageResizeService.resizeToJpeg(any(), anyInt())).willReturn(new byte[]{2});

        String key = service.storeFestivalPoster(file, LocalDate.of(2026, 8, 1));

        then(imageResizeService).should().validateFile(file);
        assertThat(key).startsWith("posters/2026-08/");
        assertThat(key).endsWith(".jpg");
        then(s3Template).should().upload(any(), eq(key), any());
    }

    @Test
    void 페스티벌_포스터_저장_시작일_없으면_연월_폴더_없음() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "poster.jpg", "image/jpeg", new byte[]{1});
        given(imageResizeService.resizeToJpeg(any(), anyInt())).willReturn(new byte[]{2});

        String key = service.storeFestivalPoster(file, null);

        assertThat(key).doesNotContain("2026");
        assertThat(key).startsWith("posters/");
    }

    // ── storeUserProfile / storeArtistProfile / storeBoothImage ──────────

    @Test
    void 유저_프로필_저장_닉네임의_특수문자는_치환됨() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});
        given(imageResizeService.resizeToJpeg(any(), anyInt())).willReturn(new byte[]{2});

        String key = service.storeUserProfile(file, "test/user*name");

        assertThat(key).contains("test_user_name");
    }

    @Test
    void 유저_프로필_저장_닉네임_없으면_unknown() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});
        given(imageResizeService.resizeToJpeg(any(), anyInt())).willReturn(new byte[]{2});

        String key = service.storeUserProfile(file, null);

        assertThat(key).contains("/unknown/");
    }

    @Test
    void 관리자_프로필_저장_결과는_buildUrl_적용된_URL() throws Exception {
        setBucketAndCdn("my-bucket", "");
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});
        given(imageResizeService.resizeToJpeg(any(), anyInt())).willReturn(new byte[]{2});

        String url = service.storeAdminProfile(file, "admin1");

        assertThat(url).startsWith("https://my-bucket.s3.ap-northeast-2.amazonaws.com/admin-profiles/");
    }

    @Test
    void 부스_이미지_저장() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "b.jpg", "image/jpeg", new byte[]{1});
        given(imageResizeService.resizeToJpeg(any(), anyInt())).willReturn(new byte[]{2});

        String key = service.storeBoothImage(file);

        assertThat(key).startsWith("booths/");
    }

    // ── deleteFile ────────────────────────────────────────────────────────

    @Test
    void 파일삭제_key가_null이면_S3_호출_안함() {
        service.deleteFile(null);

        then(s3Template).shouldHaveNoInteractions();
    }

    @Test
    void 파일삭제_key가_공백이면_S3_호출_안함() {
        service.deleteFile("  ");

        then(s3Template).shouldHaveNoInteractions();
    }

    @Test
    void 파일삭제_알수없는_외부_URL이면_S3_호출_안함() {
        setBucketAndCdn("my-bucket", "");

        service.deleteFile("https://example.com/a.jpg");

        then(s3Template).shouldHaveNoInteractions();
    }

    @Test
    void 파일삭제_S3_직접_URL이면_key_추출후_삭제() {
        setBucketAndCdn("my-bucket", "");

        service.deleteFile("https://my-bucket.s3.ap-northeast-2.amazonaws.com/admin-profiles/a.jpg");

        then(s3Template).should().deleteObject("my-bucket", "admin-profiles/a.jpg");
    }

    @Test
    void 파일삭제_CDN_URL이면_key_추출후_삭제() {
        setBucketAndCdn("my-bucket", "https://cdn.example.com/");

        service.deleteFile("https://cdn.example.com/admin-profiles/a.jpg");

        then(s3Template).should().deleteObject("my-bucket", "admin-profiles/a.jpg");
    }

    @Test
    void 파일삭제_정상_key면_S3_삭제_호출() {
        setBucketAndCdn("my-bucket", "");

        service.deleteFile("posters/a.jpg");

        then(s3Template).should().deleteObject("my-bucket", "posters/a.jpg");
    }

    @Test
    void 파일삭제_S3_예외는_무시됨() {
        setBucketAndCdn("my-bucket", "");
        doThrow(new RuntimeException("S3 오류")).when(s3Template).deleteObject(anyString(), anyString());

        service.deleteFile("posters/a.jpg");
        // 예외 없이 조용히 무시됨
    }

    // ── deleteFileAfterCommit ─────────────────────────────────────────────

    @Test
    void 커밋후삭제_활성_트랜잭션_없으면_즉시_삭제() {
        setBucketAndCdn("my-bucket", "");

        service.deleteFileAfterCommit("posters/a.jpg");

        then(s3Template).should().deleteObject("my-bucket", "posters/a.jpg");
    }

    @Test
    void 커밋후삭제_활성_트랜잭션_있으면_즉시_삭제_안하고_커밋후_삭제() {
        TransactionSynchronizationManager.initSynchronization();

        service.deleteFileAfterCommit("posters/a.jpg");

        then(s3Template).should(never()).deleteObject(anyString(), anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        then(s3Template).should().deleteObject(any(), any());
    }
}
