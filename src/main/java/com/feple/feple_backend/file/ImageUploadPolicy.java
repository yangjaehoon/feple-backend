package com.feple.feple_backend.file;

import java.util.Map;

/** 업로드 허용 이미지 확장자 ↔ Content-Type 매핑 (확장자·MIME 불일치 업로드 차단용) */
public final class ImageUploadPolicy {
    private ImageUploadPolicy() {}

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp"
    );

    public static boolean isAllowed(String extension, String contentType) {
        return contentType.equals(ALLOWED_IMAGE_TYPES.get(extension));
    }
}
