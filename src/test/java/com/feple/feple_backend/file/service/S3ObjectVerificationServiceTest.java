package com.feple.feple_backend.file.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.file.S3Properties;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class S3ObjectVerificationServiceTest {

    @Mock S3Client s3Client;

    private S3ObjectVerificationService service;

    @BeforeEach
    void setUp() {
        service = new S3ObjectVerificationService(s3Client, new S3Properties("test-bucket", 10L, 168L));
    }

    @SuppressWarnings("unchecked")
    private void stubHeadObject(HeadObjectResponse response) {
        given(s3Client.headObject(any(Consumer.class))).willReturn(response);
    }

    @Test
    void 허용된_이미지_타입이면_통과() {
        stubHeadObject(HeadObjectResponse.builder().contentType("image/jpeg").build());

        assertThatCode(() -> service.verifyImageObject("posts/1/a.jpg")).doesNotThrowAnyException();
    }

    @Test
    void 파일이_없으면_예외() {
        given(s3Client.headObject(any(java.util.function.Consumer.class))).willThrow(NoSuchKeyException.builder().build());

        assertThatThrownBy(() -> service.verifyImageObject("posts/1/a.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    void 허용되지_않는_타입이면_예외() {
        stubHeadObject(HeadObjectResponse.builder().contentType("text/plain").build());

        assertThatThrownBy(() -> service.verifyImageObject("posts/1/a.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않는");
    }

    @Test
    void contentType_없으면_예외() {
        stubHeadObject(HeadObjectResponse.builder().build());

        assertThatThrownBy(() -> service.verifyImageObject("posts/1/a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않는");
    }

    @Test
    void contentType에_세미콜론_옵션있어도_기본타입_추출() {
        stubHeadObject(HeadObjectResponse.builder().contentType("image/png; charset=utf-8").build());

        assertThatCode(() -> service.verifyImageObject("posts/1/a.png")).doesNotThrowAnyException();
    }
}
