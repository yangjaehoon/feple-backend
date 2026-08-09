package com.feple.feple_backend.user.dto;

public record NicknameAvailabilityResponse(boolean available, String code, String message) {

    private static final String INVALID_FORMAT = "INVALID_FORMAT";
    private static final String DUPLICATE = "DUPLICATE";
    private static final String AVAILABLE = "AVAILABLE";

    public static NicknameAvailabilityResponse invalidFormat(String message) {
        return new NicknameAvailabilityResponse(false, INVALID_FORMAT, message);
    }

    public static NicknameAvailabilityResponse rejected(String code, String message) {
        return new NicknameAvailabilityResponse(false, code, message);
    }

    public static NicknameAvailabilityResponse duplicate() {
        return new NicknameAvailabilityResponse(false, DUPLICATE, "이미 사용 중인 닉네임입니다.");
    }

    public static NicknameAvailabilityResponse ok() {
        return new NicknameAvailabilityResponse(true, AVAILABLE, "사용 가능한 닉네임입니다.");
    }
}
