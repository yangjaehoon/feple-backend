package com.feple.feple_backend.file.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.feple.feple_backend.file.ImageUploadPolicy;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageResizeService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final int MAX_DIMENSION_PX = 8_000;

    public void validateFile(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("파일이 비어있습니다.");

        if (file.getSize() > ImageUploadPolicy.MAX_IMAGE_UPLOAD_BYTES)
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
        int[] originalDims = readImageDimensions(bytes);

        // 원본 해상도 그대로 전체 디코딩하면 8000×8000 사진 한 장이 힙(-Xmx300m)의
        // 상당 부분을 차지할 수 있다. 실제 필요한 건 targetPx(최대 720px) 근방뿐이므로
        // 디코딩 단계에서 서브샘플링해 애초에 큰 버퍼를 만들지 않는다.
        BufferedImage src = decodeSubsampled(bytes, originalDims[0], originalDims[1], targetPx);
        if (src == null)
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");

        // ImageIO는 EXIF Orientation을 반영하지 않고 센서 방향 그대로 픽셀을 디코딩한다.
        // 세로로 촬영한 사진(특히 iPhone)이 가로로 저장되고 태그로만 회전 방향을 표시하는 경우가
        // 흔해, 회전을 픽셀에 직접 적용하지 않으면 리사이즈 후 메타데이터가 사라지며 영구히
        // 옆으로 눕는다. 리사이즈 전에 방향을 먼저 바로잡는다.
        src = applyExifOrientation(src, readExifOrientation(bytes));

        int[] dims = computeTargetSize(src.getWidth(), src.getHeight(), targetPx);
        return encodeToJpeg(src, dims[0], dims[1]);
    }

    /** targetPx에 맞는 서브샘플링 비율로 디코딩해 원본 해상도 전체를 메모리에 올리지 않는다. */
    private BufferedImage decodeSubsampled(byte[] bytes, int originalWidth, int originalHeight, int targetPx)
            throws IOException {
        int longSide = Math.max(originalWidth, originalHeight);
        int subsampling = Math.max(1, longSide / Math.max(targetPx, 1));

        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
            ImageReader reader = it.next();
            try {
                reader.setInput(iis, true, true);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    private int readExifOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (ImageProcessingException | IOException | MetadataException | RuntimeException e) {
            // EXIF가 없거나(PNG/GIF 등) 손상된 경우 방향 보정 없이 원본 그대로 처리
        }
        return 1;
    }

    /** EXIF Orientation 값(1~8)에 따라 회전/반전을 픽셀에 실제로 적용한다. */
    private BufferedImage applyExifOrientation(BufferedImage image, int orientation) {
        if (orientation <= 1 || orientation > 8) return image;

        int width = image.getWidth();
        int height = image.getHeight();
        AffineTransform t = new AffineTransform();

        switch (orientation) {
            case 2 -> {
                t.scale(-1.0, 1.0);
                t.translate(-width, 0);
            }
            case 3 -> t.rotate(Math.PI, width / 2.0, height / 2.0);
            case 4 -> {
                t.scale(1.0, -1.0);
                t.translate(0, -height);
            }
            case 5 -> {
                t.rotate(Math.PI / 2);
                t.scale(1.0, -1.0);
            }
            case 6 -> {
                t.translate(height, 0);
                t.rotate(Math.PI / 2);
            }
            case 7 -> {
                t.translate(height, width);
                t.rotate(Math.PI / 2);
                t.scale(-1.0, 1.0);
            }
            case 8 -> {
                t.translate(0, width);
                t.rotate(-Math.PI / 2);
            }
            default -> { return image; }
        }

        boolean swapDimensions = orientation >= 5;
        int newWidth = swapDimensions ? height : width;
        int newHeight = swapDimensions ? width : height;

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D graphics = rotated.createGraphics();
        graphics.setTransform(t);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rotated;
    }

    /** 헤더만 읽어 픽셀 크기를 반환 — 전체 디코딩 전에 ImageBomb 차단 + 서브샘플링 비율 계산에 재사용 */
    private int[] readImageDimensions(byte[] bytes) throws IOException {
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
                return new int[]{width, height};
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
        // getScaledInstance()는 별도의 스케일된 임시 이미지를 하나 더 할당한다 — 이미
        // 위에서 보간 힌트를 설정했으므로 drawImage에 목표 크기를 직접 넘겨 그 임시 버퍼를 없앤다.
        graphics.drawImage(src, 0, 0, width, height, null);
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", baos);
        return baos.toByteArray();
    }
}
