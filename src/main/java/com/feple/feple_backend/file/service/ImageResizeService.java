package com.feple.feple_backend.file.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class ImageResizeService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    public void validateFile(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("파일이 비어있습니다.");

        if (file.getSize() > MAX_FILE_SIZE_BYTES)
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf(".")).toLowerCase()
                : "";

        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. jpg, jpeg, png, gif 파일만 업로드할 수 있습니다.");
    }

    /** 이미지를 maxPx × maxPx 이하로 축소하여 JPEG 바이트 배열로 반환 (비율 유지) */
    public byte[] resizeToJpeg(InputStream inputStream, int maxPx) throws IOException {
        BufferedImage src = ImageIO.read(inputStream);
        if (src == null)
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");

        int w = src.getWidth(), h = src.getHeight();
        if (w > maxPx || h > maxPx) {
            double scale = Math.min((double) maxPx / w, (double) maxPx / h);
            w = (int) (w * scale);
            h = (int) (h * scale);
        }

        // PNG 투명 배경 → 흰색으로 합성 후 JPEG 변환
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(src.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", baos);
        return baos.toByteArray();
    }
}
