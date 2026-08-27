package com.feple.feple_backend.file;

import com.feple.feple_backend.global.exception.InvalidRequestException;

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
    // 사용자 노출 메시지용 — ALLOWED_IMAGE_TYPES와 어긋나지 않도록 이 목록을 유일한 출처로 삼는다.
    private static final String ALLOWED_EXTENSIONS_DISPLAY = "jpg, jpeg, png, gif, webp";

    /** 이미지 업로드 최대 크기(10MB) — 일반 multipart 업로드는 요청 시점에, presigned PUT URL은
     * Content-Length를 제한할 수 없어 업로드 후 HeadObject로 사후 검증할 때 이 값을 사용한다. */
    public static final long MAX_IMAGE_UPLOAD_BYTES = 10 * 1024 * 1024;

    public static boolean isAllowed(String extension, String contentType) {
        return contentType.equals(ALLOWED_IMAGE_TYPES.get(extension));
    }

    /** 확장자를 소문자로 정규화한 뒤 허용 여부를 검증하고, 정규화된 확장자를 반환한다.
     * 허용되지 않으면 실제 허용 목록이 반영된 메시지로 예외를 던진다. */
    public static String assertAllowed(String extension, String contentType) {
        String normalized = extension == null ? "" : extension.toLowerCase();
        if (!isAllowed(normalized, contentType)) {
            throw new InvalidRequestException("허용되지 않는 파일 형식입니다. (" + ALLOWED_EXTENSIONS_DISPLAY + " 만 가능)");
        }
        return normalized;
    }
}
