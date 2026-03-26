package com.feple.feple_backend.file;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int ARTIST_PROFILE_MAX_PX = 400;
    private static final int FESTIVAL_POSTER_MAX_PX = 720;

    private final S3Template s3Template;

    @Value("${app.s3.bucket}")
    private String bucket;

    public String buildUrl(String key) {
        if (key == null) return null;
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    public String storeFestivalPoster(MultipartFile file, LocalDate festivalStartDate) throws IOException {
        validateFile(file);

        String yearMonth = festivalStartDate == null ? ""
                : festivalStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String folder = yearMonth.isEmpty() ? "posters" : "posters/" + yearMonth;

        byte[] resized = resizeToJpeg(file.getInputStream(), FESTIVAL_POSTER_MAX_PX);
        String key = folder + "/" + UUID.randomUUID() + ".jpg";

        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    public String storeUserProfile(MultipartFile file, String nickname) throws IOException {
        validateFile(file);

        String safeName = (nickname == null || nickname.isBlank())
                ? "unknown"
                : nickname.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");

        byte[] resized = resizeToJpeg(file.getInputStream(), ARTIST_PROFILE_MAX_PX);
        String key = "user-profiles/" + safeName + "/" + UUID.randomUUID() + ".jpg";

        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    public String storeArtistProfile(MultipartFile file, String artistName) throws IOException {
        validateFile(file);

        String safeName = (artistName == null || artistName.isBlank())
                ? "unknown"
                : artistName.trim().replaceAll("[^a-zA-Z0-9가-힣_-]", "_");

        byte[] resized = resizeToJpeg(file.getInputStream(), ARTIST_PROFILE_MAX_PX);
        String key = "artists/" + safeName + "/" + UUID.randomUUID() + ".jpg";

        try (InputStream is = new ByteArrayInputStream(resized)) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    private String storeFile(MultipartFile file, String folder) throws IOException {
        validateFile(file);

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf(".")).toLowerCase()
                : "";

        String key = folder + "/" + UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            s3Template.upload(bucket, key, is);
            return key;
        }
    }

    private void validateFile(MultipartFile file) {
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
    private byte[] resizeToJpeg(InputStream inputStream, int maxPx) throws IOException {
        BufferedImage src = ImageIO.read(inputStream);
        if (src == null) throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");

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
