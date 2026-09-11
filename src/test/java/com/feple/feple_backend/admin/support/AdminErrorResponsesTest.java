package com.feple.feple_backend.admin.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.testsupport.TestImageBytes;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AdminErrorResponsesTest {

    @Test
    void 실제_PNG_바이트면_이미지로_인정() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "a.png",
                MediaType.IMAGE_PNG_VALUE, TestImageBytes.validPng());

        assertThat(AdminErrorResponses.isNotImage(file)).isFalse();
    }

    @Test
    void ContentType이_이미지가_아니라고_선언돼도_실제_바이트가_이미지면_인정() throws Exception {
        // Content-Type은 클라이언트가 임의로 조작하거나(예: PDF로 잘못 선언) 브라우저/OS에 따라
        // 엉뚱한 값을 보낼 수 있어 신뢰하지 않는다 — 판단 기준은 오직 실제 바이트 시그니처.
        MockMultipartFile file = new MockMultipartFile("image", "a.pdf",
                MediaType.APPLICATION_PDF_VALUE, TestImageBytes.validPng());

        assertThat(AdminErrorResponses.isNotImage(file)).isFalse();
    }

    @Test
    void ContentType은_image지만_실제_바이트가_이미지가_아니면_거부() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "a.jpg",
                MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        assertThat(AdminErrorResponses.isNotImage(file)).isTrue();
    }

    @Test
    void ContentType이_null이어도_실제_바이트가_이미지면_인정() throws Exception {
        // HEIC 등은 브라우저/OS에 따라 Content-Type이 아예 비거나 image/*가 아닌 값으로 올 수
        // 있다 — Content-Type 유무와 무관하게 바이트 시그니처만으로 판단해야 이런 경우도 받아들인다.
        MockMultipartFile file = new MockMultipartFile("image", "a.jpg", null, TestImageBytes.validPng());

        assertThat(AdminErrorResponses.isNotImage(file)).isFalse();
    }

    @Test
    void 파일_읽기_실패시_예외를_삼키지않고_그대로_전파한다() throws Exception {
        // "IOException(파일 디코딩·읽기 실패)→500" 컨벤션을 따르려면 호출부(controller의
        // try/catch)가 잡아 500으로 변환할 수 있도록 여기서 삼키지 않고 그대로 던져야 한다.
        MultipartFile file = mock(MultipartFile.class);
        given(file.getInputStream()).willThrow(new IOException("디스크 읽기 실패"));

        assertThatThrownBy(() -> AdminErrorResponses.isNotImage(file))
                .isInstanceOf(IOException.class);
    }

    @Test
    void ImageIO가_모르는_HEIC여도_ftyp_박스_브랜드로_이미지로_인정() throws Exception {
        // ImageIO는 HEIC를 디코딩하지 못하지만, Gemini API는 그대로 받아들인다(iPhone
        // 카메라의 기본 출력 포맷이라 실제로 들어올 수 있음) — ISO-BMFF ftyp 브랜드로 구제한다.
        byte[] heicHeader = {
                0, 0, 0, 24, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c', 0, 0, 0, 0,
        };
        MockMultipartFile file = new MockMultipartFile("image", "a.heic", "image/heic", heicHeader);

        assertThat(AdminErrorResponses.isNotImage(file)).isFalse();
    }
}
