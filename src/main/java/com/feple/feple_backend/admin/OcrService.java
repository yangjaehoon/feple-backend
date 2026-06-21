package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
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
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final GeminiOcrClient geminiOcrClient;
    private final TimetableService timetableService;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;

    public boolean isConfigured() {
        return geminiOcrClient.isConfigured();
    }

    public int getTodayUsage() {
        return geminiOcrClient.getTodayUsage();
    }

    public int getDailyLimit() {
        return geminiOcrClient.getDailyLimit();
    }

    public List<OcrResultDto> parseTimeTable(MultipartFile image) throws IOException {
        return geminiOcrClient.parseTimeTable(image);
    }

    public OcrApplyResultDto applyEntries(OcrApplyRequest request) {
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
                failures.add(buildFailureMap(entry, e.getMessage(), i));
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

    private static final String OPS_STAGE = "📢";

    private TimetableEntryRequest toTimetableRequest(OcrResultDto entry) {
        TimetableEntryRequest req = new TimetableEntryRequest();
        if (entry.isOps()) {
            req.setStageName(OPS_STAGE);
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

    public List<ArtistLineupOcrResult> parseArtistLineup(MultipartFile image) throws IOException {
        List<LineupRawResult> raw = geminiOcrClient.parseLineup(image);
        return raw.stream().map(this::matchArtist).toList();
    }

    private ArtistLineupOcrResult matchArtist(LineupRawResult raw) {
        int conf = raw.confidence() != null ? raw.confidence() : 0;
        Optional<Artist> exact = artistRepository.findExactByNameIgnoreCase(raw.name());
        if (exact.isPresent()) {
            return new ArtistLineupOcrResult(raw.name(), exact.get().getId(), exact.get().getName(), conf);
        }
        List<Artist> partial = artistRepository.findByNameOrNameEnContainingIgnoreCase(raw.name());
        if (partial.size() == 1) {
            return new ArtistLineupOcrResult(raw.name(), partial.get(0).getId(), partial.get(0).getName(), conf);
        }
        return new ArtistLineupOcrResult(raw.name(), null, null, conf);
    }

    public LineupApplyResult applyArtistLineup(Long festivalId, List<Long> artistIds) {
        int added = 0;
        int duplicates = 0;
        for (Long id : artistIds) {
            try {
                ArtistFestivalCreateRequest req = new ArtistFestivalCreateRequest();
                req.setArtistId(id);
                artistFestivalService.addArtistToFestival(festivalId, req);
                added++;
            } catch (DuplicateArtistFestivalException e) {
                duplicates++;
            }
        }
        return new LineupApplyResult(artistIds.size(), added, duplicates);
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
