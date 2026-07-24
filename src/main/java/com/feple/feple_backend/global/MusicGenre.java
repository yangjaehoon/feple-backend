package com.feple.feple_backend.global;

public enum MusicGenre {
    BAND("Band"),
    HIP_HOP("Hip-hop"),
    INDIE("Indie"),
    BALLAD("Ballad"),
    RNB("R&B"),
    DANCE("댄스"),
    IDOL("아이돌"),
    ETC("기타");

    private final String displayName;

    MusicGenre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
