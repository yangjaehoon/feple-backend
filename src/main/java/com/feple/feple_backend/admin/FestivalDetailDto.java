package com.feple.feple_backend.admin;

import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponse;

import java.util.List;
import java.util.Map;

public record FestivalDetailDto(
        FestivalResponseDto festival,
        List<ArtistFestivalResponse> participatingArtists,
        List<ArtistFestivalResponse> participatingArtistsByName,
        List<TimetableEntryResponse> timetableEntries,
        Map<String, List<TimetableEntryResponse>> timetableByArtist,
        List<Stage> stages,
        List<BoothResponseDto> booths,
        BoothType[] allBoothTypes,
        String googleMapsKey
) {}
