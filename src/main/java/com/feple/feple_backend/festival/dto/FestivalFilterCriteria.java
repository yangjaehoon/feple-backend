package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;

import java.util.List;

public record FestivalFilterCriteria(
        List<Genre> genres,
        List<Region> regions,
        List<AgeRestriction> ageRestrictions,
        boolean includeEnded,
        String sort
) {
    public static FestivalFilterCriteria forAdmin() {
        return new FestivalFilterCriteria(null, null, null, true, null);
    }
}
