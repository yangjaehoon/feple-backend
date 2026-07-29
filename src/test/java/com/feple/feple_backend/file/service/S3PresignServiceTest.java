package com.feple.feple_backend.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import java.net.URL;
import java.time.Duration;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3PresignServiceTest {

    @Mock S3Presigner s3Presigner;

    @InjectMocks S3PresignService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
        ReflectionTestUtils.setField(service, "presignMinutes", 10L);
        ReflectionTestUtils.setField(service, "getPresignHours", 168L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void presignPut_업로드용_URL과_objectKey_반환() throws Exception {
        PutObjectPresignRequest.Builder builder = mock(PutObjectPresignRequest.Builder.class, RETURNS_SELF);
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(new URL("https://s3.example.com/upload"));
        given(s3Presigner.presignPutObject(any(Consumer.class))).willAnswer(invocation -> {
            Consumer<PutObjectPresignRequest.Builder> consumer = invocation.getArgument(0);
            consumer.accept(builder);
            return presigned;
        });

        S3PresignedUrlResult result = service.presignPut("artist-photos/1/a.jpg", "image/jpeg");

        assertThat(result.uploadUrl()).isEqualTo("https://s3.example.com/upload");
        assertThat(result.objectKey()).isEqualTo("artist-photos/1/a.jpg");
        verify(builder).signatureDuration(Duration.ofMinutes(10));
        verify(builder).putObjectRequest(any(PutObjectRequest.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void presignGetUrl_조회용_URL_반환() throws Exception {
        GetObjectPresignRequest.Builder builder = mock(GetObjectPresignRequest.Builder.class, RETURNS_SELF);
        GetObjectRequest.Builder goBuilder = mock(GetObjectRequest.Builder.class, RETURNS_SELF);
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        given(presigned.url()).willReturn(new URL("https://s3.example.com/download"));
        given(builder.getObjectRequest(any(Consumer.class))).willAnswer(invocation -> {
            Consumer<GetObjectRequest.Builder> goConsumer = invocation.getArgument(0);
            goConsumer.accept(goBuilder);
            return builder;
        });
        given(s3Presigner.presignGetObject(any(Consumer.class))).willAnswer(invocation -> {
            Consumer<GetObjectPresignRequest.Builder> consumer = invocation.getArgument(0);
            consumer.accept(builder);
            return presigned;
        });

        String url = service.presignGetUrl("artist-photos/1/a.jpg");

        assertThat(url).isEqualTo("https://s3.example.com/download");
        verify(builder).signatureDuration(Duration.ofHours(168));
        verify(goBuilder).bucket("test-bucket");
        verify(goBuilder).key("artist-photos/1/a.jpg");
    }
}
