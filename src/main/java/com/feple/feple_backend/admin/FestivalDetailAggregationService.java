package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponse;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalDetailAggregationService {

    private static final String ANNOUNCEMENT_STAGE = "📢";

    private final FestivalService festivalService;
    private final ArtistFestivalService artistFestivalService;
    private final TimetableService timetableService;
    private final StageService stageService;
    private final BoothService boothService;
    private final SongAdminService songAdminService;

    @Value("${app.google.maps.key:}")
    private String googleMapsKey;

    public FestivalDetailDto buildAttributes(Long festivalId) {
        FestivalResponseDto festival = festivalService.getFestival(festivalId);

        List<ArtistFestivalResponse> participatingArtists =
                artistFestivalService.getArtistFestivals(festivalId);

        List<ArtistFestivalResponse> participatingArtistsByName = participatingArtists.stream()
                .sorted(java.util.Comparator.comparing(
                        ArtistFestivalResponse::getArtistName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<TimetableEntryResponse> timetableEntries = timetableService.getEntries(festivalId);
        Map<String, List<TimetableEntryResponse>> timetableByArtist = timetableEntries.stream()
                .filter(e -> e.getArtistName() != null && !e.getArtistName().isBlank()
                             && !ANNOUNCEMENT_STAGE.equals(e.getStageName()))
                .collect(Collectors.groupingBy(TimetableEntryResponse::getArtistName));

        List<Long> afIds = participatingArtists.stream()
                .map(ArtistFestivalResponse::getArtistFestivalId)
                .toList();
        Map<Long, Integer> setlistCounts = afIds.isEmpty()
                ? Collections.emptyMap()
                : songAdminService.getSetlistCounts(afIds);

        return new FestivalDetailDto(
                festival,
                participatingArtists,
                participatingArtistsByName,
                timetableEntries,
                timetableByArtist,
                stageService.getStages(festivalId),
                boothService.getBooths(festivalId),
                BoothType.values(),
                googleMapsKey,
                setlistCounts
        );
    }

    public void populateModel(Long festivalId, Model model) {
        FestivalDetailDto detail = buildAttributes(festivalId);
        model.addAttribute("festival",                 detail.festival());
        model.addAttribute("participatingArtists",     detail.participatingArtists());
        model.addAttribute("participatingArtistsByName", detail.participatingArtistsByName());
        model.addAttribute("timetableEntries",         detail.timetableEntries());
        model.addAttribute("timetableByArtist",        detail.timetableByArtist());
        model.addAttribute("stages",                   detail.stages());
        model.addAttribute("booths",                   detail.booths());
        model.addAttribute("allBoothTypes",            detail.allBoothTypes());
        model.addAttribute("googleMapsKey",            detail.googleMapsKey());
        model.addAttribute("setlistCounts",            detail.setlistCounts());
    }
}
