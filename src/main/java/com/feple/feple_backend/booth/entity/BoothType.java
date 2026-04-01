package com.feple.feple_backend.booth.entity;

public enum BoothType {
    FOOD("음식"),
    BEER("주류"),
    EVENT("이벤트");

    private final String displayName;

    BoothType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
