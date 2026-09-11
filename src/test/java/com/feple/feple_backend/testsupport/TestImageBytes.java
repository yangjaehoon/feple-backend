package com.feple.feple_backend.testsupport;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;

/** ImageIO 매직바이트 검증(AdminErrorResponses.isNotImage 등)을 통과해야 하는 테스트를 위해,
 * 실제로 디코딩 가능한 최소 크기 PNG 바이트를 생성한다. */
public final class TestImageBytes {

    private TestImageBytes() {}

    public static byte[] validPng() {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
