package com.feple.feple_backend.admin.system;

import java.time.LocalDateTime;

public record SongRequestSummaryDto(
        Long id,
        String songTitle,
        String artistName,
        LocalDateTime createdAt
) {
    public static SongRequestSummaryDto from(com.feple.feple_backend.artist.song.entity.SongRequest req) {
        return new SongRequestSummaryDto(req.getId(), req.getSongTitle(), req.getArtistName(), req.getCreatedAt());
    }
}
