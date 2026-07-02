package com.feple.feple_backend.user;

import java.util.regex.Pattern;

public final class NicknameValidator {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9_]+$");

    private NicknameValidator() {}

    public static void validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        String trimmed = nickname.trim();
        if (trimmed.length() < 2 || trimmed.length() > 8) {
            throw new IllegalArgumentException("닉네임은 2자 이상 8자 이하로 입력해주세요.");
        }
        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        }
    }
}
