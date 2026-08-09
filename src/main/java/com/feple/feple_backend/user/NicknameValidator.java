package com.feple.feple_backend.user;

import java.util.regex.Pattern;

public final class NicknameValidator {

    public static final int MIN_NICKNAME_LENGTH = 2;
    public static final int MAX_NICKNAME_LENGTH = 8;

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9_]+$");

    private NicknameValidator() {}

    public static void validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        String trimmed = nickname.trim();
        if (trimmed.length() < MIN_NICKNAME_LENGTH || trimmed.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("닉네임은 2자 이상 8자 이하로 입력해주세요.");
        }
        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        }
    }
}
