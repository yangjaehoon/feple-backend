package com.feple.feple_backend.artist.entity;

public enum ArtistGenre {
    BAND("Band"),
    HIP_HOP("Hip-hop"),
    INDIE("Indie"),
    BALLAD("Ballad"),
    RNB("R&B");

    private final String displayName;

    ArtistGenre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
