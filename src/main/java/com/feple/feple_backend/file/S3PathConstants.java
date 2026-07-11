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
}
