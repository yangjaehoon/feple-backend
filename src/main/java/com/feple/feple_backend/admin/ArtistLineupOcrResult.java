package com.feple.feple_backend.admin;

public record ArtistLineupOcrResult(
        String parsedName,
        Long artistId,
        String matchedName,
        int confidence
) {
}
