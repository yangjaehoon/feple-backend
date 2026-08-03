package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponseDto;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistScheduleService {

    private final ArtistFestivalRepository artistFestivalRepository;
    private final FileStorageService fileStorageService;

    public List<ArtistScheduleResponseDto> getArtistSchedule(Long artistId) {
        List<ArtistFestival> myFestivals =
                artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(artistId);

        // 페스티벌 ID 목록을 한 번에 조회하여 N+1 방지
        List<Long> festivalIds = myFestivals.stream()
                .map(ArtistFestival::getFestivalId)
                .toList();

        Map<Long, List<ArtistFestival>> coArtistMap = festivalIds.isEmpty()
                ? Map.of()
                : artistFestivalRepository.findByFestivalIdInWithArtist(festivalIds)
                        .stream()
                        .collect(Collectors.groupingBy(ArtistFestival::getFestivalId));

        return myFestivals.stream()
                .map(af -> buildResponse(af, artistId, coArtistMap))
                .toList();
    }

    private ArtistScheduleResponseDto buildResponse(ArtistFestival af, Long artistId,
                                                  Map<Long, List<ArtistFestival>> coArtistMap) {
        List<ArtistScheduleResponseDto.CoArtistInfo> coArtists = buildCoArtists(af, artistId, coArtistMap);
        LocalDate performanceDate = af.getPerformanceDate();

        return ArtistScheduleResponseDto.builder()
                .festivalId(af.getFestivalId())
                .title(af.getFestivalTitle())
                .description(af.getFestivalDescription())
                .location(af.getFestivalLocation())
                .startDate(performanceDate != null ? performanceDate : af.getFestivalStartDate())
                .endDate(performanceDate != null ? performanceDate : af.getFestivalEndDate())
                .posterUrl(fileStorageService.buildUrl(af.getFestivalPosterKey()))
                .eventType(af.getFestivalEventType())
                .coArtists(coArtists)
                .build();
    }

    private List<ArtistScheduleResponseDto.CoArtistInfo> buildCoArtists(ArtistFestival af, Long artistId,
                                                  Map<Long, List<ArtistFestival>> coArtistMap) {
        return coArtistMap.getOrDefault(af.getFestivalId(), List.of())
                .stream()
                .filter(other -> !other.getArtistId().equals(artistId))
                .map(other -> ArtistScheduleResponseDto.CoArtistInfo.builder()
                        .artistId(other.getArtistId())
                        .artistName(other.getArtistName())
                        .artistNameEn(other.getArtistNameEn())
                        .profileImageUrl(fileStorageService.buildUrl(
                                other.getArtistProfileImageKey()))
                        .build())
                .toList();
    }
}
