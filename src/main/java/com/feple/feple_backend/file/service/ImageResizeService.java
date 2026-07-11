package com.feple.feple_backend.file.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

@Service
public class ImageResizeService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_DIMENSION_PX = 8_000;

    public void validateFile(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("파일이 비어있습니다.");

        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank())
            throw new IllegalArgumentException("파일 이름이 없습니다.");

        // 더블 확장자 공격 방지 (.jpg.exe 등)
        String nameWithoutExt = original.contains(".")
                ? original.substring(0, original.lastIndexOf("."))
                : original;
        if (nameWithoutExt.contains("."))
            throw new IllegalArgumentException("다중 확장자 파일은 업로드할 수 없습니다.");

        String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. jpg, jpeg, png, gif 파일만 업로드할 수 있습니다.");
    }

    /** 이미지를 targetPx × targetPx 이하로 축소하여 JPEG 바이트 배열로 반환 (비율 유지) */
    public byte[] resizeToJpeg(InputStream inputStream, int targetPx) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        validateImageDimensions(bytes);

        BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
        if (src == null)
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");

        int[] dims = computeTargetSize(src.getWidth(), src.getHeight(), targetPx);
        return encodeToJpeg(src, dims[0], dims[1]);
    }

    /** 헤더만 읽어 픽셀 크기 검증 — 전체 디코딩 전에 ImageBomb 차단 */
    private void validateImageDimensions(byte[] bytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
            ImageReader reader = it.next();
            try {
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_DIMENSION_PX || height > MAX_DIMENSION_PX)
                    throw new IllegalArgumentException(
                        "이미지 크기가 너무 큽니다. 최대 " + MAX_DIMENSION_PX + "×" + MAX_DIMENSION_PX + " 픽셀까지 허용됩니다.");
            } finally {
                reader.dispose();
            }
        }
    }

    private int[] computeTargetSize(int width, int height, int maxPx) {
        if (width <= maxPx && height <= maxPx) return new int[]{width, height};
        double scale = Math.min((double) maxPx / width, (double) maxPx / height);
        return new int[]{(int) (width * scale), (int) (height * scale)};
    }

    /**
     * PNG 투명 배경을 흰색으로 합성한 뒤 JPEG로 인코딩.
     * ImageIO.write()는 IIOMetadata 없이 픽셀 데이터만 JPEG로 직렬화하므로
     * 원본의 EXIF(GPS, 카메라 정보 등) 및 ICC 프로파일이 출력에 포함되지 않는다.
     */
    private byte[] encodeToJpeg(BufferedImage src, int width, int height) throws IOException {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(src.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", baos);
        return baos.toByteArray();
    }
}
