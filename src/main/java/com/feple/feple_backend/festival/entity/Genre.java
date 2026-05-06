package com.feple.feple_backend.festival.entity;

public enum Genre {
    HIP_HOP("Hip-hop"),
    INDIE("Indie"),
    BAND("Band"),
    ETC("기타");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
