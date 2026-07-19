package com.feple.feple_backend.file;

public final class S3PathConstants {
    private S3PathConstants() {}

    public static final String POSTERS = "posters";
    public static final String USER_PROFILES = "user-profiles";
    public static final String ARTISTS = "artists";
    public static final String BOOTHS = "booths";

    public static String certificationPrefix(Long userId) {
        return "certifications/" + userId + "/";
    }

    public static String artistPhotoPrefix(Long artistId) {
        return "artist-photos/" + artistId + "/";
    }

    /** 클라이언트가 제출한 오브젝트 키가 서버가 발급한 presign prefix 범위 안인지 검증 */
    public static void requireWithinPrefix(String objectKey, String prefix) {
        if (objectKey == null || !objectKey.startsWith(prefix)) {
            throw new IllegalArgumentException("잘못된 오브젝트 키입니다.");
        }
    }
}
