package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public FestivalDetailDto getDetail(Long festivalId) {
        FestivalResponseDto festival = festivalService.getFestival(festivalId);
        List<TimetableEntryResponseDto> entries = timetableService.getEntries(festivalId);

        // 타임테이블 1회만 로드 — 아티스트 참여 정보와 타임테이블 그리드 양쪽에서 재사용
        Map<String, List<String>> datesByArtistName = buildDatesByArtistName(entries);
        Map<String, String> stageByArtistName = buildStageByArtistName(entries);
        List<ArtistFestivalResponseDto> artists = artistFestivalService.getArtistFestivals(festivalId, datesByArtistName, stageByArtistName);

        return new FestivalDetailDto(
                festival,
                artists,
                sortArtistsByName(artists),
                entries,
                buildTimetableByArtist(artists, entries),
                stageService.getStages(festivalId),
                boothService.getBooths(festivalId),
                BoothType.values(),
                googleMapsKey,
                buildSetlistCounts(artists),
                ANNOUNCEMENT_STAGE
        );
    }

    private Map<String, String> buildStageByArtistName(List<TimetableEntryResponseDto> entries) {
        return entries.stream()
                .filter(e -> e.getArtistName() != null && !e.getArtistName().isBlank()
                             && e.getStageName() != null && !e.getStageName().isBlank())
                .collect(Collectors.toMap(
                        TimetableEntryResponseDto::getArtistName,
                        TimetableEntryResponseDto::getStageName,
                        (s1, s2) -> s1
                ));
    }

    private Map<String, List<String>> buildDatesByArtistName(List<TimetableEntryResponseDto> entries) {
        return entries.stream()
                .filter(e -> e.getArtistName() != null && !e.getArtistName().isBlank())
                .collect(Collectors.groupingBy(
                        TimetableEntryResponseDto::getArtistName,
                        Collectors.mapping(TimetableEntryResponseDto::getFestivalDate, Collectors.toList())
                ));
    }

    private Map<String, List<TimetableEntryResponseDto>> buildTimetableByArtist(
            List<ArtistFestivalResponseDto> artists, List<TimetableEntryResponseDto> entries) {
        Map<String, List<TimetableEntryResponseDto>> result = entries.stream()
                .filter(e -> e.getArtistName() != null && !e.getArtistName().isBlank()
                             && !ANNOUNCEMENT_STAGE.equals(e.getStageName()))
                .collect(Collectors.groupingBy(TimetableEntryResponseDto::getArtistName,
                        HashMap::new, Collectors.toList()));
        artists.forEach(a -> result.putIfAbsent(a.getArtistName(), List.of()));
        return result;
    }

    private List<ArtistFestivalResponseDto> sortArtistsByName(List<ArtistFestivalResponseDto> artists) {
        return artists.stream()
                .sorted(Comparator.comparing(ArtistFestivalResponseDto::getArtistName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<Long, Integer> buildSetlistCounts(List<ArtistFestivalResponseDto> artists) {
        List<Long> afIds = artists.stream().map(ArtistFestivalResponseDto::getArtistFestivalId).toList();
        return afIds.isEmpty() ? Collections.emptyMap() : songAdminService.getSetlistCounts(afIds);
    }
}
