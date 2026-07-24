package com.feple.feple_backend.admin.ocr;

public record ArtistLineupOcrResult(
        String parsedName,
        Long artistId,
        String matchedName,
        int confidence
) {
}
