package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponse;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistScheduleService {

    private final ArtistFestivalRepository artistFestivalRepository;
    private final FileStorageService fileStorageService;

    public List<ArtistScheduleResponse> getArtistSchedule(Long artistId) {
        List<ArtistFestival> myFestivals =
                artistFestivalRepository.findByArtistIdOrderByFestivalStartDateDesc(artistId);

        // 페스티벌 ID 목록을 한 번에 조회하여 N+1 방지
        List<Long> festivalIds = myFestivals.stream()
                .map(af -> af.getFestival().getId())
                .toList();

        Map<Long, List<ArtistFestival>> coArtistMap = festivalIds.isEmpty()
                ? Map.of()
                : artistFestivalRepository.findByFestivalIdInWithArtist(festivalIds)
                        .stream()
                        .collect(Collectors.groupingBy(af -> af.getFestival().getId()));

        return myFestivals.stream()
                .map(af -> buildResponse(af, artistId, coArtistMap))
                .toList();
    }

    private ArtistScheduleResponse buildResponse(ArtistFestival af, Long artistId,
                                                  Map<Long, List<ArtistFestival>> coArtistMap) {
        Festival festival = af.getFestival();
        List<ArtistScheduleResponse.CoArtistInfo> coArtists =
                coArtistMap.getOrDefault(festival.getId(), List.of())
                        .stream()
                        .filter(other -> !other.getArtist().getId().equals(artistId))
                        .map(other -> ArtistScheduleResponse.CoArtistInfo.builder()
                                .artistId(other.getArtist().getId())
                                .artistName(other.getArtist().getName())
                                .profileImageUrl(fileStorageService.buildUrl(
                                        other.getArtist().getProfileImageKey()))
                                .build())
                        .toList();

        return ArtistScheduleResponse.builder()
                .festivalId(festival.getId())
                .title(festival.getTitle())
                .description(festival.getDescription())
                .location(festival.getLocation())
                .startDate(festival.getStartDate())
                .endDate(festival.getEndDate())
                .posterUrl(fileStorageService.buildUrl(festival.getPosterKey()))
                .eventType(festival.getEventType())
                .coArtists(coArtists)
                .build();
    }
}
