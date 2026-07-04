package com.feple.feple_backend.artistfestival.event;

public record ArtistAddedToFestivalEvent(
        Long artistId,
        String artistName,
        String artistNameEn,
        Long festivalId,
        String festivalTitle,
        String festivalTitleEn
) {}
