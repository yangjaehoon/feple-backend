package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
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
    private final StageRepository stageRepository;
    private final NotificationService notificationService;

    public List<ArtistFestivalResponse> getArtistFestivals(Long festivalId) {
        List<ArtistFestival> artistFestivals =
                artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId);

        // 타임테이블에서 artistName → performanceDates 맵 빌드 (N+1 방지)
        Map<String, List<String>> datesByArtistName = timetableRepository
                .findByFestivalIdWithStage(festivalId)
                .stream()
                .collect(Collectors.groupingBy(
                        TimetableEntry::getArtistName,
                        Collectors.mapping(
                                e -> e.getFestivalDate().toString(),
                                Collectors.toList()
                        )
                ));

        return artistFestivals.stream()
                .map(af -> toResponse(af, datesByArtistName.getOrDefault(
                        af.getArtistName(), List.of())))
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

        // 비동기 알림 발송 — 아직 시작 전인 페스티벌에만 발송
        if (festival.getStartDate() != null && festival.getStartDate().isAfter(java.time.LocalDate.now())) {
            notificationService.notifyNewFestivalForArtist(
                    artist.getId(), artist.getName(),
                    festival.getId(), festival.getTitle());
        }

        return saved.getId();
    }

    @Transactional
    public void updateArtistFestival(Long festivalId, Long artistFestivalId,
                                     Integer lineupOrder, String stageName) {
        ArtistFestival af = artistFestivalRepository.findById(artistFestivalId)
                .orElseThrow(() -> new NoSuchElementException("참여 정보가 없습니다."));
        if (!af.getFestivalId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        // 스테이지가 변경되면 해당 아티스트의 타임테이블 스테이지도 함께 업데이트
        String oldStage = af.getStageName();
        af.updateLineup(lineupOrder, stageName);

        if (stageName != null && !stageName.equals(oldStage)) {
            String artistName = af.getArtistName();
            Stage newStage = stageRepository.findByFestivalIdAndName(festivalId, stageName)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 스테이지입니다: " + stageName));
            timetableRepository.findByFestivalIdAndArtistName(festivalId, artistName)
                    .forEach(entry -> entry.updateStage(newStage));
        }
    }

    @Transactional
    public void removeArtistFromFestival(Long festivalId, Long artistFestivalId) {
        ArtistFestival artistFestival = artistFestivalRepository.findById(artistFestivalId)
                .orElseThrow(() -> new NoSuchElementException("참여 정보가 없습니다."));

        if (!artistFestival.getFestivalId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        artistFestivalRepository.delete(artistFestival);
    }


    private ArtistFestivalResponse toResponse(ArtistFestival af, List<String> dates) {
        return ArtistFestivalResponse.builder()
                .artistFestivalId(af.getId())
                .artistId(af.getArtistId())
                .artistName(af.getArtistName())
                .artistGenre(af.getArtistGenreDisplayName())
                .profileImageUrl(fileStorageService.buildUrl(af.getArtistProfileImageKey()))
                .lineupOrder(af.getLineupOrder())
                .stageName(af.getStageName())
                .performanceDates(dates)
                .build();
    }
}