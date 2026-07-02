package com.feple.feple_backend.artist.song.event;

public record SongRequestRejectedEvent(
        Long userId,
        Long artistId,
        String songTitle,
        String artistName,
        String reason
) {}
