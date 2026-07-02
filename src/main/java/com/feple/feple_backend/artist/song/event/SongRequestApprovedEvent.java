package com.feple.feple_backend.artist.song.event;

public record SongRequestApprovedEvent(
        Long userId,
        Long artistId,
        String songTitle,
        String artistName
) {}
