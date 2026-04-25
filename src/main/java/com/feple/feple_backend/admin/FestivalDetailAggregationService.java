package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalDetailAggregationService {

    private final FestivalService festivalService;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;
    private final TimetableService timetableService;
    private final StageService stageService;
    private final BoothService boothService;

    @Value("${app.google.maps.key:}")
    private String googleMapsKey;

    /** 페스티벌 상세 뷰에 필요한 모든 데이터를 집계하여 반환 */
    public Map<String, Object> buildAttributes(Long festivalId) {
        FestivalResponseDto festival = festivalService.getFestival(festivalId);
        List<Artist> allArtists = artistRepository.findAll(Sort.by("name"));

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
                             && !"📢".equals(e.getStageName()))
                .collect(Collectors.groupingBy(TimetableEntryResponse::getArtistName));

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("festival", festival);
        attrs.put("allArtists", allArtists);
        attrs.put("participatingArtists", participatingArtists);
        attrs.put("participatingArtistsByName", participatingArtistsByName);
        attrs.put("timetableEntries", timetableEntries);
        attrs.put("timetableByArtist", timetableByArtist);
        attrs.put("stages", stageService.getStages(festivalId));
        attrs.put("booths", boothService.getBooths(festivalId));
        attrs.put("allBoothTypes", BoothType.values());
        attrs.put("googleMapsKey", googleMapsKey);
        return attrs;
    }
}
