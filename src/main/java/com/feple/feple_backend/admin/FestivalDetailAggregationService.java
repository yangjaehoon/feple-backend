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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void populateModel(Long festivalId, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(festivalId);
        List<TimetableEntryResponse> entries = timetableService.getEntries(festivalId);

        // 타임테이블 1회만 로드 — 아티스트 참여 정보와 타임테이블 그리드 양쪽에서 재사용
        Map<String, List<String>> datesByArtistName = buildDatesByArtistName(entries);
        List<ArtistFestivalResponse> artists = artistFestivalService.getArtistFestivals(festivalId, datesByArtistName);

        model.addAttribute("festival",                   festival);
        model.addAttribute("participatingArtists",       artists);
        model.addAttribute("participatingArtistsByName", sortArtistsByName(artists));
        model.addAttribute("timetableEntries",           entries);
        model.addAttribute("timetableByArtist",          buildTimetableByArtist(artists, entries));
        model.addAttribute("stages",                     stageService.getStages(festivalId));
        model.addAttribute("booths",                     boothService.getBooths(festivalId));
        model.addAttribute("allBoothTypes",              BoothType.values());
        model.addAttribute("googleMapsKey",              googleMapsKey);
        model.addAttribute("setlistCounts",              buildSetlistCounts(artists));
        model.addAttribute("opsStageIndicator",          ANNOUNCEMENT_STAGE);
    }

    private Map<String, List<String>> buildDatesByArtistName(List<TimetableEntryResponse> entries) {
        return entries.stream()
                .filter(e -> e.getArtistName() != null && !e.getArtistName().isBlank())
                .collect(Collectors.groupingBy(
                        TimetableEntryResponse::getArtistName,
                        Collectors.mapping(TimetableEntryResponse::getFestivalDate, Collectors.toList())
                ));
    }

    private Map<String, List<TimetableEntryResponse>> buildTimetableByArtist(
            List<ArtistFestivalResponse> artists, List<TimetableEntryResponse> entries) {
        Map<String, List<TimetableEntryResponse>> result = entries.stream()
                .filter(e -> e.getArtistName() != null && !e.getArtistName().isBlank()
                             && !ANNOUNCEMENT_STAGE.equals(e.getStageName()))
                .collect(Collectors.groupingBy(TimetableEntryResponse::getArtistName,
                        HashMap::new, Collectors.toList()));
        artists.forEach(a -> result.putIfAbsent(a.getArtistName(), List.of()));
        return result;
    }

    private List<ArtistFestivalResponse> sortArtistsByName(List<ArtistFestivalResponse> artists) {
        return artists.stream()
                .sorted(Comparator.comparing(ArtistFestivalResponse::getArtistName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<Long, Integer> buildSetlistCounts(List<ArtistFestivalResponse> artists) {
        List<Long> afIds = artists.stream().map(ArtistFestivalResponse::getArtistFestivalId).toList();
        return afIds.isEmpty() ? Collections.emptyMap() : songAdminService.getSetlistCounts(afIds);
    }
}
