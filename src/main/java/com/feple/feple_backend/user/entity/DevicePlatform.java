package com.feple.feple_backend.user.entity;

public enum DevicePlatform {
    ANDROID, IOS;

    public static DevicePlatform from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("지원하지 않는 디바이스 플랫폼입니다: null");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 디바이스 플랫폼입니다: " + value);
        }
    }
}
