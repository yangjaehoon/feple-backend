package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.artist.song.service.SetlistAdminService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.service.TimetableService;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalDetailAggregationService {


    private final FestivalAdminService festivalService;
    private final ArtistFestivalService artistFestivalService;
    private final TimetableService timetableService;
    private final StageService stageService;
    private final BoothService boothService;
    private final SetlistAdminService setlistAdminService;
    private final FestivalReviewService reviewService;

    @Value("${app.google.maps.key:}")
    private String googleMapsKey;

    public FestivalDetailDto getDetail(Long festivalId) {
        if (googleMapsKey == null || googleMapsKey.isBlank()) {
            // 브라우저 콘솔에만 에러가 남고 서버 로그엔 흔적이 없어 놓치기 쉬움
            log.warn("[Maps] GOOGLE_MAPS_KEY가 설정되지 않아 관리자 페이지 지도가 표시되지 않습니다.");
        }
        FestivalResponseDto festival = festivalService.getFestival(festivalId);
        List<TimetableEntryResponseDto> entries = timetableService.getEntries(festivalId);

        // 타임테이블 1회만 로드 — 아티스트 참여 정보와 타임테이블 그리드 양쪽에서 재사용
        Map<String, List<String>> datesByArtistName = buildDatesByArtistName(entries);
        Map<String, String> stageByArtistName = buildStageByArtistName(entries);
        List<ArtistFestivalResponseDto> artists = artistFestivalService.getArtistFestivalsWithStageFallback(festivalId, datesByArtistName, stageByArtistName);

        double avg = reviewService.getAverageRating(festivalId);
        int cnt = reviewService.getRatingCount(festivalId);
        FestivalRatingStatsDto ratingStats = cnt > 0
                ? new FestivalRatingStatsDto(avg, cnt, reviewService.getRatingDistribution(festivalId))
                : FestivalRatingStatsDto.EMPTY;

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
                TimetableEntry.ANNOUNCEMENT_SENTINEL,
                ratingStats
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
                             && !TimetableEntry.ANNOUNCEMENT_SENTINEL.equals(e.getStageName()))
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
        return afIds.isEmpty() ? Collections.emptyMap() : setlistAdminService.getSetlistCounts(afIds);
    }
}
