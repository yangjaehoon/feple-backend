package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.entity.LineupUpdate;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.entity.TimetableEntryFields;
import com.feple.feple_backend.timetable.entity.TimetableEntryMember;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableService {

    // 이 시간대에 걸친 start→end는 자정을 넘기는 심야 공연으로 간주해 허용한다
    // (예: 23:30 시작 → 00:30 종료). TimetableEntry는 종료일 필드가 없어 festivalDate 하루 안에서
    // wall-clock 시간만으로 심야 여부를 판단한다. 정오 이전에 시작해서 자정을 넘기는 공연은
    // 사실상 없다고 보고, 이보다 이른 시각의 start/end 역전은 입력 실수로 간주해 그대로 거부한다.
    private static final LocalTime OVERNIGHT_START_THRESHOLD = LocalTime.of(12, 0);
    private static final LocalTime OVERNIGHT_END_THRESHOLD = LocalTime.of(6, 0);

    private final TimetableRepository timetableRepository;
    private final FestivalRepository festivalRepository;
    private final StageService stageService;
    private final ArtistFestivalService artistFestivalService;
    private final ArtistRepository artistRepository;
    private final TimetableEntryBatchPersister entryBatchPersister;

    @Transactional(readOnly = true)
    @Cacheable(value = "timetable", key = "#festivalId")
    public List<TimetableEntryResponseDto> getEntries(Long festivalId) {
        return timetableRepository.findByFestivalIdWithStage(festivalId)
                .stream()
                .map(TimetableEntryResponseDto::from)
                .sorted(Comparator
                        .comparing(TimetableEntryResponseDto::getFestivalDate)
                        .thenComparingInt(TimetableEntryResponseDto::getStageOrder)
                        .thenComparing(TimetableEntryResponseDto::getStartTime))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public TimetableEntryResponseDto createEntry(Long festivalId, TimetableEntryRequestDto req) {
        Festival festival = getFestivalOrThrow(festivalId);
        return createEntry(festival, req);
    }

    // OCR 일괄 적용처럼 같은 festivalId로 여러 엔트리를 연속 생성하는 호출부가 매 엔트리마다
    // festival을 재조회하지 않도록, 호출부가 한 번만 조회한 Festival을 재사용할 수 있게 공개한다.
    @Transactional
    @CacheEvict(value = "timetable", key = "#festival.id")
    public TimetableEntryResponseDto createEntry(Festival festival, TimetableEntryRequestDto req) {
        Long festivalId = festival.getId();
        validateTimeRange(req);
        StageResolution stageResolution = resolveStage(festivalId, req.getStageName());

        String color = (req.getColor() != null && !req.getColor().isBlank()) ? req.getColor().trim() : null;
        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stage(stageResolution.stage())
                .stageName(stageResolution.stageName())
                .artistName(req.getArtistName() != null ? req.getArtistName().trim() : "")
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .color(color)
                .build();
        TimetableEntry saved = timetableRepository.save(entry);
        syncMembers(saved, req.getMemberArtistIds());
        broadcastLineupUpdate(festivalId, saved);
        return TimetableEntryResponseDto.from(saved);
    }

    public Festival getFestivalOrThrow(Long festivalId) {
        return EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
    }

    public record BatchCreateResult(TimetableEntry entry, RuntimeException error) {}

    // createEntry(Festival, req)의 배치 버전 — OCR 일괄 적용(60~100건)이 항목마다 스테이지 조회
    // (resolveStage)와 라인업 역동기화(syncFromTimetableEntry) 쿼리를 반복하지 않도록, festival의
    // 스테이지 목록을 한 번만 조회해 맵으로 매칭하고 라인업 동기화도 전부 모았다가 한 번에 처리한다.
    // 항목별 실패는 예외를 던지지 않고 결과에 담아, 앞선 성공 건이 뒤 항목의 실패로 롤백되지 않게 한다.
    @Transactional
    @CacheEvict(value = "timetable", key = "#festival.id")
    public List<BatchCreateResult> createEntriesBatch(Festival festival, List<TimetableEntryRequestDto> reqs) {
        Long festivalId = festival.getId();
        Map<String, Stage> stagesByName = stageService.getStages(festivalId).stream()
                .collect(Collectors.toMap(Stage::getName, s -> s, (a, b) -> a));

        List<BatchCreateResult> results = new ArrayList<>();
        List<ArtistFestivalService.ArtistNameLineup> lineupUpdates = new ArrayList<>();
        for (TimetableEntryRequestDto req : reqs) {
            try {
                validateTimeRange(req);
                String stageName = (req.getStageName() == null || req.getStageName().isBlank())
                        ? "" : req.getStageName().trim();
                Stage stage = stageName.isEmpty() ? null : stagesByName.get(stageName);
                // 항목마다 독립 트랜잭션으로 저장 — 하나가 DB 제약 위반 등으로 실패해도
                // 영속성 컨텍스트 오염이 다른 항목에 전파되지 않는다 (자세한 이유는
                // TimetableEntryBatchPersister 클래스 주석 참고)
                TimetableEntry saved = entryBatchPersister.saveIsolated(festival, stage, stageName, req);

                LineupUpdate lineup = new LineupUpdate(saved.getStageName(), saved.getFestivalDate());
                lineupUpdates.add(new ArtistFestivalService.ArtistNameLineup(saved.getArtistName(), lineup));
                for (TimetableEntryMember member : saved.getMembers()) {
                    lineupUpdates.add(new ArtistFestivalService.ArtistNameLineup(member.getArtistName(), lineup));
                }
                results.add(new BatchCreateResult(saved, null));
            } catch (RuntimeException e) {
                results.add(new BatchCreateResult(null, e));
            }
        }
        artistFestivalService.syncFromTimetableEntriesBatch(festivalId, lineupUpdates);
        return results;
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public void updateEntry(Long festivalId, Long entryId, TimetableEntryRequestDto req) {
        TimetableEntry entry = EntityLoader.getOrThrow(timetableRepository::findById, entryId, "타임테이블 항목");
        EntityLoader.requireBelongsToFestival(festivalId, entry.getFestivalId(), "항목이");
        validateTimeRange(req);
        StageResolution stageResolution = resolveStage(festivalId, req.getStageName());
        entry.update(new TimetableEntryFields(
                req.getArtistName() != null ? req.getArtistName().trim() : "",
                stageResolution.stageName(),
                stageResolution.stage(),
                req.getFestivalDate(),
                req.getStartTime(),
                req.getEndTime(),
                req.getColor()));
        syncMembers(entry, req.getMemberArtistIds());
        broadcastLineupUpdate(festivalId, entry);
    }

    private record StageResolution(String stageName, Stage stage) {}

    private StageResolution resolveStage(Long festivalId, String rawStageName) {
        String stageName = (rawStageName == null || rawStageName.isBlank()) ? "" : rawStageName.trim();
        Stage stage = stageName.isEmpty() ? null : stageService.findByFestivalIdAndName(festivalId, stageName).orElse(null);
        return new StageResolution(stageName, stage);
    }

    private void validateTimeRange(TimetableEntryRequestDto req) {
        LocalTime start = req.getStartTime();
        LocalTime end = req.getEndTime();
        if (isOvernight(start, end)) return;
        if (!start.isBefore(end)) {
            throw new InvalidRequestException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
    }

    private boolean isOvernight(LocalTime start, LocalTime end) {
        return !start.isBefore(OVERNIGHT_START_THRESHOLD) && !end.isAfter(OVERNIGHT_END_THRESHOLD);
    }

    private void broadcastLineupUpdate(Long festivalId, TimetableEntry entry) {
        LineupUpdate lineup = new LineupUpdate(entry.getStageName(), entry.getFestivalDate());
        artistFestivalService.syncFromTimetableEntry(festivalId, entry.getArtistName(), lineup);
        for (TimetableEntryMember member : entry.getMembers()) {
            artistFestivalService.syncFromTimetableEntry(festivalId, member.getArtistName(), lineup);
        }
    }

    private void syncMembers(TimetableEntry entry, List<Long> memberArtistIds) {
        TimetableMemberSync.sync(artistRepository, entry, memberArtistIds);
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public void deleteEntry(Long festivalId, Long entryId) {
        TimetableEntry entry = EntityLoader.getOrThrow(timetableRepository::findById, entryId, "타임테이블 항목");
        EntityLoader.requireBelongsToFestival(festivalId, entry.getFestivalId(), "항목이");
        timetableRepository.delete(entry);
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public void removeAllByFestival(Long festivalId) {
        timetableRepository.deleteByFestivalId(festivalId);
    }
}
