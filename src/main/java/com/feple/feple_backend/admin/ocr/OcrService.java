package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final GeminiOcrClient geminiOcrClient;
    private final TimetableService timetableService;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;
    private final UnmatchedArtistSuggestionService suggestionService;

    public boolean isConfigured() {
        return geminiOcrClient.isConfigured();
    }

    public int getTodayUsage() {
        return geminiOcrClient.getTodayUsage();
    }

    public int getDailyLimit() {
        return geminiOcrClient.getDailyLimit();
    }

    public OcrParseResult<OcrResultDto> parseTimeTable(MultipartFile image, Integer year) throws IOException {
        return geminiOcrClient.parseTimeTable(image, year);
    }

    public OcrApplyResultDto applyEntries(OcrApplyRequestDto request) {
        List<Map<String, String>> failures = new ArrayList<>();
        int savedCount = 0;

        List<OcrResultDto> entries = request.entries();
        for (int i = 0; i < entries.size(); i++) {
            OcrResultDto entry = entries.get(i);
            Optional<String> error = validateEntry(entry);
            if (error.isPresent()) {
                failures.add(buildFailureMap(entry, error.get(), i));
                continue;
            }
            try {
                timetableService.createEntry(request.festivalId(), toTimetableRequest(entry));
                savedCount++;
            } catch (Exception e) {
                // 사용자에게 드러낼 수 있는 검증 오류만 메시지 전달, 내부 예외는 고정 문구 사용
                String reason = (e instanceof IllegalArgumentException || e instanceof NoSuchElementException)
                        ? e.getMessage()
                        : "처리 중 오류 발생";
                failures.add(buildFailureMap(entry, reason, i));
            }
        }
        return new OcrApplyResultDto(savedCount, failures.size(), failures);
    }

    private Optional<String> validateEntry(OcrResultDto entry) {
        if (entry.date() == null || entry.date().isBlank())
            return Optional.of("날짜 누락");
        if (entry.startTime() == null || entry.endTime() == null)
            return Optional.of("시작/종료 시간 누락");
        try {
            LocalDate.parse(entry.date());
        } catch (java.time.format.DateTimeParseException ex) {
            return Optional.of("날짜 형식 오류: " + entry.date());
        }
        return Optional.empty();
    }

    private TimetableEntryRequestDto toTimetableRequest(OcrResultDto entry) {
        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        if (entry.isOps()) {
            req.setStageName(TimetableEntry.ANNOUNCEMENT_STAGE_NAME);
        } else {
            req.setStageName(entry.stage() != null ? entry.stage().trim() : "");
        }
        req.setArtistName(entry.artist() != null ? entry.artist().trim() : "");
        req.setFestivalDate(LocalDate.parse(entry.date()));
        req.setStartTime(LocalTime.parse(entry.startTime()));
        req.setEndTime(LocalTime.parse(entry.endTime()));
        return req;
    }

    // ── 라인업 OCR ──────────────────────────────────────────

    public OcrParseResult<ArtistLineupOcrResult> parseArtistLineup(MultipartFile image) throws IOException {
        OcrParseResult<LineupRawResult> raw = geminiOcrClient.parseLineup(image);
        List<ArtistLineupOcrResult> matched = raw.entries().stream().map(this::matchArtist).toList();
        return new OcrParseResult<>(matched, raw.truncated());
    }

    private ArtistLineupOcrResult matchArtist(LineupRawResult raw) {
        int conf = raw.confidence() != null ? raw.confidence() : 0;
        Optional<Artist> exact = artistRepository.findExactByNameIgnoreCase(raw.name());
        if (exact.isPresent()) {
            Artist artist = exact.get();
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), conf);
        }
        List<Artist> partial = artistRepository.findByNameOrNameEnContainingIgnoreCase(raw.name());
        if (partial.size() == 1) {
            Artist artist = partial.get(0);
            return new ArtistLineupOcrResult(raw.name(), artist.getId(), artist.getName(), conf);
        }
        return new ArtistLineupOcrResult(raw.name(), null, null, conf);
    }

    // @Transactional 제거: addArtistToFestival(ConflictException 발생 시)이 외부 트랜잭션을
    // rollback-only로 마킹해 UnexpectedRollbackException이 발생하는 것을 방지.
    // 각 addArtistToFestival 호출은 자신의 독립 트랜잭션을 사용함.
    public LineupApplyResult applyArtistLineup(LineupOcrApplyRequestDto request) {
        int added = 0;
        int duplicates = 0;
        for (Long id : request.artistIds()) {
            try {
                ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
                req.setArtistId(id);
                artistFestivalService.addArtistToFestival(request.festivalId(), req);
                added++;
            } catch (ConflictException e) {
                duplicates++;
            }
        }
        if (request.unmatchedNames() != null) {
            suggestionService.saveAll(request.unmatchedNames());
        }
        return new LineupApplyResult(request.artistIds().size(), added, duplicates);
    }

    public List<UnmatchedArtistSuggestionDto> getSuggestions() {
        return suggestionService.getAll();
    }

    public void deleteSuggestion(Long id) {
        suggestionService.delete(id);
    }

    // ── 타임테이블 OCR ──────────────────────────────────────

    private Map<String, String> buildFailureMap(OcrResultDto entry, String reason, int index) {
        Map<String, String> map = new HashMap<>();
        map.put("index",  String.valueOf(index));
        map.put("artist", Objects.requireNonNullElse(entry.artist(), "—"));
        map.put("stage",  Objects.requireNonNullElse(entry.stage(),  "—"));
        map.put("reason", Objects.requireNonNullElse(reason, "알 수 없는 오류"));
        return map;
    }
}
