package com.feple.feple_backend.user.entity;

import com.feple.feple_backend.global.exception.InvalidRequestException;

public enum DevicePlatform {
    ANDROID, IOS;

    // 클라이언트가 platform을 안 보내는 경우의 기본값 — ANDROID와 동일한 값이 유지되도록 name()에서 파생시킨다
    public static final String DEFAULT = ANDROID.name().toLowerCase();

    public static DevicePlatform from(String value) {
        if (value == null) {
            throw new InvalidRequestException("지원하지 않는 디바이스 플랫폼입니다: null");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("지원하지 않는 디바이스 플랫폼입니다: " + value);
        }
    }
}
