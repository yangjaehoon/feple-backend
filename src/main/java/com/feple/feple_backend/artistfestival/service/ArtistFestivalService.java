package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponse;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistFestivalService {

    private final ArtistFestivalRepository artistFestivalRepository;
    private final FestivalRepository festivalRepository;
    private final ArtistRepository artistRepository;
    private final FileStorageService fileStorageService;
    private final TimetableRepository timetableRepository;
    private final NotificationService notificationService;

    public List<ArtistScheduleResponse> getArtistSchedule(Long artistId) {
        List<ArtistFestival> myFestivals = artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(artistId);

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
                .map(af -> {
                    Festival festival = af.getFestival();
                    List<ArtistScheduleResponse.CoArtistInfo> coArtists =
                            coArtistMap.getOrDefault(festival.getId(), List.of())
                                    .stream()
                                    .filter(other -> !other.getArtist().getId().equals(artistId))
                                    .map(other -> ArtistScheduleResponse.CoArtistInfo.builder()
                                            .artistId(other.getArtist().getId())
                                            .artistName(other.getArtist().getName())
                                            .profileImageUrl(fileStorageService.buildUrl(other.getArtist().getProfileImageKey()))
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
                })
                .toList();
    }

    public List<ArtistFestivalResponse> getArtistFestivals(Long festivalId) {
        return artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public Long addArtistToFestival(Long festivalId, ArtistFestivalCreateRequest request) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌을 찾을 수 없습니다."));

        Artist artist = artistRepository.findById(request.getArtistId())
                .orElseThrow(() -> new NoSuchElementException("아티스트를 찾을 수 없습니다."));

        if (artistFestivalRepository.existsByFestivalIdAndArtistId(festivalId, request.getArtistId())) {
            throw new DuplicateArtistFestivalException();
        }

        ArtistFestival artistFestival = ArtistFestival.builder()
                .festival(festival)
                .artist(artist)
                .lineupOrder(request.getLineupOrder())
                .stageName(request.getStageName())
                .build();

        ArtistFestival saved = artistFestivalRepository.save(artistFestival);

        // 비동기 알림 발송 — 아티스트 팔로워들에게 새 페스티벌 알림
        notificationService.notifyNewFestivalForArtist(
                artist.getId(), artist.getName(),
                festival.getId(), festival.getTitle());

        return saved.getId();
    }

    @Transactional
    public void updateArtistFestival(Long festivalId, Long artistFestivalId,
                                     Integer lineupOrder, String stageName) {
        ArtistFestival af = artistFestivalRepository.findById(artistFestivalId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 없습니다."));
        if (!af.getFestival().getId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        // 스테이지가 변경되면 해당 아티스트의 타임테이블 스테이지도 함께 업데이트
        String oldStage = af.getStageName();
        af.updateLineup(lineupOrder, stageName);

        if (stageName != null && !stageName.equals(oldStage)) {
            String artistName = af.getArtist().getName();
            timetableRepository.findByFestivalIdAndArtistName(festivalId, artistName)
                    .forEach(entry -> entry.setStageName(stageName));
        }
    }

    @Transactional
    public void removeArtistFromFestival(Long festivalId, Long artistFestivalId) {
        ArtistFestival artistFestival = artistFestivalRepository.findById(artistFestivalId)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 없습니다."));

        if (!artistFestival.getFestival().getId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        artistFestivalRepository.delete(artistFestival);
    }


    private ArtistFestivalResponse toResponse(ArtistFestival af) {
        return ArtistFestivalResponse.builder()
                .artistFestivalId(af.getId())
                .artistId(af.getArtist().getId())
                .artistName(af.getArtist().getName())
                .artistGenre(af.getArtist().getGenre() != null ? af.getArtist().getGenre().getDisplayName() : null)
                .profileImageUrl(fileStorageService.buildUrl(af.getArtist().getProfileImageKey()))
                .lineupOrder(af.getLineupOrder())
                .stageName(af.getStageName())
                .build();
    }
}