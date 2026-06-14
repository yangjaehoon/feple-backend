package com.feple.feple_backend.artist.song.event;

public record SongRequestApprovedEvent(
        Long userId,
        String songTitle,
        String artistName
) {}
