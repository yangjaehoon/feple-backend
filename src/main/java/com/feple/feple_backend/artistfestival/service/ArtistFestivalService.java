package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.dto.ArtistNameOption;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.entity.LineupUpdate;
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.KoreaClock;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import com.feple.feple_backend.timetable.service.TimetableSyncService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistFestivalService {

    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFestivalSongRepository artistFestivalSongRepository;
    private final FestivalRepository festivalRepository;
    private final ArtistRepository artistRepository;
    private final FileStorageService fileStorageService;
    private final TimetableRepository timetableRepository;
    private final TimetableSyncService timetableSyncService;
    private final ApplicationEventPublisher eventPublisher;

    public List<ArtistFestivalResponseDto> getArtistFestivals(Long festivalId) {
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

        return toResponseList(artistFestivals, datesByArtistName);
    }

    // 관리자 상세 페이지 전용 — 타임테이블 기반 스테이지/날짜 폴백 적용
    public List<ArtistFestivalResponseDto> getArtistFestivalsWithStageFallback(Long festivalId,
                                                           Map<String, List<String>> datesByArtistName,
                                                           Map<String, String> stageByArtistName) {
        return artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId).stream()
                .map(af -> toResponse(af,
                        datesByArtistName.getOrDefault(af.getArtistName(), List.of()),
                        stageByArtistName.getOrDefault(af.getArtistName(), null)))
                .toList();
    }

    private List<ArtistFestivalResponseDto> toResponseList(List<ArtistFestival> artistFestivals,
                                                             Map<String, List<String>> datesByArtistName) {
        return artistFestivals.stream()
                .map(af -> toResponse(af, datesByArtistName.getOrDefault(af.getArtistName(), List.of())))
                .toList();
    }

    @Transactional
    public Long addArtistToFestival(Long festivalId, ArtistFestivalCreateRequestDto request) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, request.getArtistId(), "아티스트");

        if (artistFestivalRepository.existsByFestivalIdAndArtistId(festivalId, request.getArtistId())) {
            throw new ConflictException("이미 이 페스티벌에 참여 중인 아티스트입니다.");
        }

        ArtistFestival artistFestival = ArtistFestival.builder()
                .festival(festival)
                .artist(artist)
                .lineupOrder(request.getLineupOrder())
                .stageName(request.getStageName())
                .build();

        ArtistFestival saved = artistFestivalRepository.save(artistFestival);

        // 트랜잭션 커밋 후에만 알림 발송 — 아직 시작 전인 페스티벌에만 발송
        if (isBeforeFestivalStart(festival)) {
            publishArtistAddedEvent(artist, festival);
        }

        return saved.getId();
    }

    // 페스티벌/아티스트/기존 참여 여부를 아티스트 수만큼 반복 조회하지 않고 한 번씩만 조회한다
    // (OCR 라인업 일괄 등록·관리자 페이지 아티스트 일괄 추가에서 공통으로 사용).
    @Transactional
    public LinkArtistsResult linkArtistsToFestival(Long festivalId, List<Long> artistIds) {
        if (artistIds == null || artistIds.isEmpty()) return new LinkArtistsResult(0, 0, 0);

        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> existingArtistIds = artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId)
                .stream().map(ArtistFestival::getArtistId).collect(Collectors.toSet());
        Map<Long, Artist> artistsById = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, a -> a));

        ArtistLinkBatch batch = classifyArtistsToLink(festivalId, artistIds, artistsById, existingArtistIds, festival);
        artistFestivalRepository.saveAll(batch.toSave());

        // 트랜잭션 커밋 후에만 알림 발송 — 아직 시작 전인 페스티벌에만 발송
        if (isBeforeFestivalStart(festival)) {
            batch.addedArtists().forEach(artist -> publishArtistAddedEvent(artist, festival));
        }
        return new LinkArtistsResult(batch.addedArtists().size(), batch.duplicates(), batch.errors());
    }

    private record ArtistLinkBatch(List<ArtistFestival> toSave, List<Artist> addedArtists,
                                    int duplicates, int errors) {}

    private ArtistLinkBatch classifyArtistsToLink(Long festivalId, List<Long> artistIds, Map<Long, Artist> artistsById,
                                                   Set<Long> existingArtistIds, Festival festival) {
        int duplicates = 0, errors = 0;
        List<ArtistFestival> toSave = new ArrayList<>();
        List<Artist> addedArtists = new ArrayList<>();
        for (Long artistId : artistIds) {
            Artist artist = artistsById.get(artistId);
            if (artist == null) {
                log.debug("[ArtistFestival] 존재하지 않는 아티스트라 건너뜀 festivalId={}, artistId={}", festivalId, artistId);
                errors++;
                continue;
            }
            if (!existingArtistIds.add(artistId)) {
                duplicates++;
                continue;
            }
            toSave.add(ArtistFestival.builder().festival(festival).artist(artist).build());
            addedArtists.add(artist);
        }
        return new ArtistLinkBatch(toSave, addedArtists, duplicates, errors);
    }

    public record LinkArtistsResult(int added, int duplicates, int errors) {}

    private boolean isBeforeFestivalStart(Festival festival) {
        return festival.getStartDate() != null
                && festival.getStartDate().isAfter(KoreaClock.today());
    }

    private void publishArtistAddedEvent(Artist artist, Festival festival) {
        eventPublisher.publishEvent(new ArtistAddedToFestivalEvent(
                artist.getId(), artist.getName(), artist.getNameEn(),
                festival.getId(), festival.getTitle(), festival.getTitleEn()));
    }

    @Transactional
    public void updateArtistFestival(Long festivalId, Long artistFestivalId, LineupUpdate lineup) {
        ArtistFestival af = EntityLoader.getOrThrow(artistFestivalRepository::findById, artistFestivalId, "참여 정보");
        assertBelongsToFestival(af, festivalId);
        applyLineupUpdate(festivalId, af, lineup);
    }

    // updateArtistFestival/updateArtistFestivalsBatch/removeArtistFromFestival 공통 —
    // 다른 페스티벌 소속 참여 정보에 접근/수정하는 것을 막는다.
    private static boolean belongsToFestival(ArtistFestival af, Long festivalId) {
        return af.getFestivalId().equals(festivalId);
    }

    private static void assertBelongsToFestival(ArtistFestival af, Long festivalId) {
        if (!belongsToFestival(af, festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }
    }

    // 라인업 그리드 일괄 수정 — 행마다 findById를 반복하지 않고 한 번에 조회한다.
    @Transactional
    public BatchUpdateResult updateArtistFestivalsBatch(Long festivalId, Map<Long, LineupUpdate> updates) {
        if (updates.isEmpty()) return new BatchUpdateResult(0, 0);

        Map<Long, ArtistFestival> byId = artistFestivalRepository.findAllById(updates.keySet()).stream()
                .collect(Collectors.toMap(ArtistFestival::getId, af -> af));

        int success = 0, errors = 0;
        for (Map.Entry<Long, LineupUpdate> entry : updates.entrySet()) {
            ArtistFestival af = byId.get(entry.getKey());
            if (af == null || !belongsToFestival(af, festivalId)) {
                errors++;
                continue;
            }
            try {
                applyLineupUpdate(festivalId, af, entry.getValue());
                success++;
            } catch (Exception e) {
                log.warn("batchUpdateLineup 실패: festivalId={}, afId={}", festivalId, entry.getKey(), e);
                errors++;
            }
        }
        return new BatchUpdateResult(success, errors);
    }

    public record BatchUpdateResult(int success, int errors) {}

    // 라인업(스테이지/날짜) 수정 + 타임테이블 역방향 동기화 — updateArtistFestival/updateArtistFestivalsBatch 공통
    private void applyLineupUpdate(Long festivalId, ArtistFestival af, LineupUpdate lineup) {
        String resolvedStage = resolveStage(lineup.stageName());
        String oldStage = af.getStageName();
        LocalDate oldDate = af.getPerformanceDate();
        af.updateLineup(resolvedStage, lineup.date());

        String artistName = af.getArtistName();
        timetableSyncService.syncStage(festivalId, artistName, resolvedStage, oldStage);
        timetableSyncService.syncDate(festivalId, artistName, lineup.date(), oldDate);
    }

    // 타임테이블 항목 저장 후 ArtistFestival 날짜·스테이지 역방향 동기화
    @Transactional
    public void syncFromTimetableEntry(Long festivalId, String artistName, LineupUpdate lineup) {
        if (artistName == null || artistName.isBlank()) return;
        artistFestivalRepository.findByFestivalIdAndArtistName(festivalId, artistName)
                .ifPresent(af -> af.updateLineup(resolveStage(lineup.stageName()), lineup.date()));
    }

    // 빈 문자열("미지정" 선택)은 null로 정규화
    private String resolveStage(String stageName) {
        return (stageName != null && !stageName.isBlank()) ? stageName : null;
    }

    public List<ArtistNameOption> getArtistFestivalsWithEnName(Long festivalId) {
        return artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId)
                .stream()
                .map(af -> new ArtistNameOption(af.getArtistName(), af.getArtistNameEn() != null ? af.getArtistNameEn() : ""))
                .toList();
    }

    public List<ArtistFestival> getAppearancesByArtistId(Long artistId) {
        return artistFestivalRepository.findByArtistIdOrderByFestivalStartDateDesc(artistId);
    }

    public boolean existsByIdAndArtistId(Long artistFestivalId, Long artistId) {
        return artistFestivalRepository.existsByIdAndArtistId(artistFestivalId, artistId);
    }

    public ArtistFestival getArtistFestivalByIdAndArtistId(Long artistFestivalId, Long artistId) {
        if (!artistFestivalRepository.existsByIdAndArtistId(artistFestivalId, artistId)) {
            throw new IllegalArgumentException("해당 아티스트의 셋리스트가 아닙니다.");
        }
        return EntityLoader.getOrThrow(artistFestivalRepository::findByIdWithFestival, artistFestivalId, "아티스트 페스티벌");
    }

    @Transactional
    public void removeArtistFromFestival(Long festivalId, Long artistFestivalId) {
        ArtistFestival artistFestival = EntityLoader.getOrThrow(artistFestivalRepository::findById, artistFestivalId, "참여 정보");
        assertBelongsToFestival(artistFestival, festivalId);
        artistFestivalRepository.delete(artistFestival);
    }

    /** 페스티벌 삭제 시 아티스트 참여 정보 일괄 제거 — 셋리스트 곡(자식 엔티티)을 먼저 정리(FK 순서) */
    @Transactional
    public void removeAllByFestival(Long festivalId) {
        artistFestivalSongRepository.deleteByFestivalId(festivalId);
        artistFestivalRepository.deleteByFestivalId(festivalId);
    }

    /** 아티스트 삭제 시 참여 정보 일괄 제거 — 셋리스트 곡(자식 엔티티)을 먼저 정리(FK 순서) */
    @Transactional
    public void removeAllByArtist(Long artistId) {
        List<Long> artistFestivalIds = artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(artistId)
                .stream().map(ArtistFestival::getId).toList();
        if (!artistFestivalIds.isEmpty()) {
            artistFestivalSongRepository.deleteByArtistFestivalIdIn(artistFestivalIds);
        }
        artistFestivalRepository.deleteByArtistId(artistId);
    }

    private ArtistFestivalResponseDto toResponse(ArtistFestival af, List<String> dates) {
        return toResponse(af, dates, null);
    }

    private ArtistFestivalResponseDto toResponse(ArtistFestival af, List<String> dates, String stageFallback) {
        String stage = af.getStageName() != null ? af.getStageName() : stageFallback;
        String date = af.getPerformanceDate() != null
                ? af.getPerformanceDate().toString()
                : (dates.isEmpty() ? null : dates.get(0));
        return ArtistFestivalResponseDto.builder()
                .artistFestivalId(af.getId())
                .artistId(af.getArtistId())
                .artistName(af.getArtistName())
                .artistNameEn(af.getArtistNameEn())
                .artistGenre(af.getArtistGenreDisplayName())
                .profileImageUrl(fileStorageService.buildUrl(af.getArtistProfileImageKey()))
                .lineupOrder(af.getLineupOrder())
                .stageName(stage)
                .performanceDate(date)
                .performanceDates(dates)
                .build();
    }
}
