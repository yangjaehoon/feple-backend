package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.global.MusicGenre;

public record ArtistAdminListQuery(
        String sort,
        String keyword,
        MusicGenre genre,
        int page
) {}
