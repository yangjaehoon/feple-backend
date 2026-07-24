package com.feple.feple_backend.user.entity;

public enum DevicePlatform {
    ANDROID, IOS;

    public static DevicePlatform from(String value) {
        if (value == null) return ANDROID;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return ANDROID; }
    }
}
