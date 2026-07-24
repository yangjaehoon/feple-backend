package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;

import java.util.List;
import java.util.Map;

public record FestivalDetailDto(
        FestivalResponseDto festival,
        List<ArtistFestivalResponseDto> participatingArtists,
        List<ArtistFestivalResponseDto> participatingArtistsByName,
        List<TimetableEntryResponseDto> timetableEntries,
        Map<String, List<TimetableEntryResponseDto>> timetableByArtist,
        List<Stage> stages,
        List<BoothResponseDto> booths,
        BoothType[] allBoothTypes,
        String googleMapsKey,
        Map<Long, Integer> setlistCounts,
        String announcementStageName,
        FestivalRatingStatsDto ratingStats
) {}
