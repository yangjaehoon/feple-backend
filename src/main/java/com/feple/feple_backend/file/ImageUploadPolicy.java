package com.feple.feple_backend.file;

import java.util.Map;

/** 업로드 허용 이미지 확장자 ↔ Content-Type 매핑 (확장자·MIME 불일치 업로드 차단용) */
public final class ImageUploadPolicy {
    private ImageUploadPolicy() {}

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "gif",  "image/gif",
            "webp", "image/webp"
    );

    /** 이미지 업로드 최대 크기(10MB) — 일반 multipart 업로드는 요청 시점에, presigned PUT URL은
     * Content-Length를 제한할 수 없어 업로드 후 HeadObject로 사후 검증할 때 이 값을 사용한다. */
    public static final long MAX_IMAGE_UPLOAD_BYTES = 10 * 1024 * 1024;

    public static boolean isAllowed(String extension, String contentType) {
        return contentType.equals(ALLOWED_IMAGE_TYPES.get(extension));
    }
}
