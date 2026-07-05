package com.feple.feple_backend.file.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageResizeServiceTest {

    private final ImageResizeService service = new ImageResizeService();

    private byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    /** 실제 1x1 PNG를 만든 뒤 IHDR의 width/height만 큰 값으로 조작 — 거대한 픽셀 데이터를 실제로 만들지 않고도 헤더 검증 로직만 테스트한다. */
    private byte[] pngWithFakeDimensions(int fakeWidth, int fakeHeight) throws IOException {
        byte[] bytes = pngBytes(1, 1);
        // PNG 시그니처(8바이트) 이후: length(4) + "IHDR"(4) + width(4) + height(4) + ...
        writeIntAt(bytes, 16, fakeWidth);
        writeIntAt(bytes, 20, fakeHeight);

        CRC32 crc = new CRC32();
        crc.update(bytes, 12, 17); // "IHDR" + 13바이트 데이터
        writeIntAt(bytes, 29, (int) crc.getValue());
        return bytes;
    }

    private void writeIntAt(byte[] bytes, int offset, int value) {
        bytes[offset]     = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    // ── validateFile ──────────────────────────────────────────────────────

    @Test
    void validateFile_빈_파일이면_예외() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일이 비어있습니다.");
    }

    @Test
    void validateFile_10MB_초과하면_예외() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[10 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10MB를 초과할 수 없습니다.");
    }

    @Test
    void validateFile_파일명_없으면_예외() {
        MockMultipartFile file = new MockMultipartFile("file", null, "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 이름이 없습니다.");
    }

    @Test
    void validateFile_다중_확장자면_예외() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg.exe", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("다중 확장자 파일은 업로드할 수 없습니다.");
    }

    @Test
    void validateFile_허용되지_않는_확장자면_예외() {
        MockMultipartFile file = new MockMultipartFile("file", "a.bmp", "image/bmp", new byte[]{1});

        assertThatThrownBy(() -> service.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 파일 형식입니다.");
    }

    @Test
    void validateFile_허용된_확장자면_통과() {
        for (String ext : new String[]{"jpg", "jpeg", "png", "gif", "JPG", "PNG"}) {
            MockMultipartFile file = new MockMultipartFile("file", "photo." + ext, "image/jpeg", new byte[]{1});
            assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
        }
    }

    // ── resizeToJpeg ──────────────────────────────────────────────────────

    @Test
    void resizeToJpeg_maxPx보다_작으면_원본_크기_유지() throws IOException {
        byte[] png = pngBytes(50, 30);

        byte[] result = service.resizeToJpeg(new ByteArrayInputStream(png), 100);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isEqualTo(50);
        assertThat(decoded.getHeight()).isEqualTo(30);
    }

    @Test
    void resizeToJpeg_maxPx보다_크면_비율_유지하며_축소() throws IOException {
        byte[] png = pngBytes(200, 100);

        byte[] result = service.resizeToJpeg(new ByteArrayInputStream(png), 100);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(decoded.getWidth()).isEqualTo(100);
        assertThat(decoded.getHeight()).isEqualTo(50);
    }

    @Test
    void resizeToJpeg_이미지가_아니면_예외() {
        byte[] notAnImage = "이건 이미지가 아닙니다".getBytes();

        assertThatThrownBy(() -> service.resizeToJpeg(new ByteArrayInputStream(notAnImage), 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resizeToJpeg_헤더상_크기가_제한을_초과하면_전체_디코딩_없이_예외() throws IOException {
        byte[] fakePng = pngWithFakeDimensions(9000, 9000);

        assertThatThrownBy(() -> service.resizeToJpeg(new ByteArrayInputStream(fakePng), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 크기가 너무 큽니다");
    }
}
