package com.feple.feple_backend.festival.entity;

public enum AgeRestriction {
    NONE("전체 관람가"),
    AGE_12("만 12세 이상"),
    AGE_15("만 15세 이상"),
    AGE_19("만 19세 이상");

    private final String displayName;

    AgeRestriction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
