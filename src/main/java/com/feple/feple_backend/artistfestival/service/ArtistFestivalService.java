package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.global.EntityLoader;

import java.time.LocalDate;
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

        return getArtistFestivals(artistFestivals, datesByArtistName);
    }

    // 타임테이블을 이미 로드한 경우 재사용 — 중복 쿼리 방지
    public List<ArtistFestivalResponseDto> getArtistFestivals(Long festivalId,
                                                           Map<String, List<String>> datesByArtistName) {
        return getArtistFestivals(
                artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId),
                datesByArtistName);
    }

    // 관리자 상세 페이지 전용 — 타임테이블 기반 스테이지/날짜 폴백 적용
    public List<ArtistFestivalResponseDto> getArtistFestivals(Long festivalId,
                                                           Map<String, List<String>> datesByArtistName,
                                                           Map<String, String> stageByArtistName) {
        return artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId).stream()
                .map(af -> toResponse(af,
                        datesByArtistName.getOrDefault(af.getArtistName(), List.of()),
                        stageByArtistName.getOrDefault(af.getArtistName(), null)))
                .toList();
    }

    private List<ArtistFestivalResponseDto> getArtistFestivals(List<ArtistFestival> artistFestivals,
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
        if (festival.getStartDate() != null && festival.getStartDate().isAfter(java.time.LocalDate.now())) {
            eventPublisher.publishEvent(new ArtistAddedToFestivalEvent(
                    artist.getId(), artist.getName(), artist.getNameEn(),
                    festival.getId(), festival.getTitle(), festival.getTitleEn()));
        }

        return saved.getId();
    }

    @Transactional
    public void linkArtistsToFestival(Long festivalId, List<Long> artistIds) {
        if (artistIds == null || artistIds.isEmpty()) return;
        for (Long artistId : artistIds) {
            try {
                ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
                req.setArtistId(artistId);
                addArtistToFestival(festivalId, req);
            } catch (ConflictException | NoSuchElementException ignored) {
            }
        }
    }

    @Transactional
    public void updateArtistFestival(Long festivalId, Long artistFestivalId,
                                     String stageName, LocalDate performanceDate) {
        ArtistFestival af = EntityLoader.getOrThrow(artistFestivalRepository::findById, artistFestivalId, "참여 정보");
        if (!af.getFestivalId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        // 빈 문자열("미지정" 선택)은 null로 정규화
        String resolvedStage = (stageName != null && !stageName.isBlank()) ? stageName : null;
        String oldStage = af.getStageName();
        LocalDate oldDate = af.getPerformanceDate();
        af.updateLineup(resolvedStage, performanceDate);

        String artistName = af.getArtistName();

        // 스테이지가 변경되면 해당 아티스트의 타임테이블 스테이지도 함께 업데이트
        if (resolvedStage != null && !resolvedStage.equals(oldStage)) {
            Stage newStage = EntityLoader.getOrThrow(
                    name -> stageRepository.findByFestivalIdAndName(festivalId, name), resolvedStage, "스테이지");
            timetableRepository.findByFestivalIdAndArtistName(festivalId, artistName)
                    .forEach(entry -> entry.updateStage(newStage));
        }

        // 날짜가 변경되면 해당 아티스트의 타임테이블 날짜도 함께 업데이트
        if (performanceDate != null && !performanceDate.equals(oldDate)) {
            List<com.feple.feple_backend.timetable.entity.TimetableEntry> entries =
                    timetableRepository.findByFestivalIdAndArtistName(festivalId, artistName);
            if (oldDate != null) {
                entries.stream()
                        .filter(e -> oldDate.equals(e.getFestivalDate()))
                        .forEach(e -> e.updateDate(performanceDate));
            } else {
                entries.forEach(e -> e.updateDate(performanceDate));
            }
        }
    }

    // 타임테이블 항목 저장 후 ArtistFestival 날짜·스테이지 역방향 동기화
    @Transactional
    public void syncFromTimetableEntry(Long festivalId, String artistName, LocalDate date, String stageName) {
        if (artistName == null || artistName.isBlank()) return;
        artistFestivalRepository.findByFestivalIdAndArtistName(festivalId, artistName)
                .ifPresent(af -> {
                    String resolvedStage = (stageName != null && !stageName.isBlank()) ? stageName : null;
                    af.updateLineup(resolvedStage, date);
                });
    }

    public List<Map<String, Object>> getArtistFestivalsWithEnName(Long festivalId) {
        return artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId)
                .stream()
                .map(af -> {
                    Map<String, Object> artistInfo = new java.util.HashMap<>();
                    artistInfo.put("name",   af.getArtistName());
                    artistInfo.put("nameEn", af.getArtistNameEn() != null ? af.getArtistNameEn() : "");
                    return artistInfo;
                })
                .toList();
    }

    public List<ArtistFestival> getAppearancesByArtistId(Long artistId) {
        return artistFestivalRepository.findByArtistIdOrderByFestivalStartDateDesc(artistId);
    }

    public ArtistFestival getArtistFestivalById(Long id) {
        return EntityLoader.getOrThrow(artistFestivalRepository::findByIdWithFestival, id, "아티스트 페스티벌");
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

        if (!artistFestival.getFestivalId().equals(festivalId)) {
            throw new IllegalArgumentException("잘못된 페스티벌입니다.");
        }

        artistFestivalRepository.delete(artistFestival);
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