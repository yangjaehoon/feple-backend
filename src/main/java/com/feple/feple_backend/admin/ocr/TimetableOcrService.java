package com.feple.feple_backend.admin.ocr;

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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TimetableOcrService {

    private final GeminiOcrClient geminiOcrClient;
    private final TimetableService timetableService;

    public boolean isConfigured() {
        return geminiOcrClient.isConfigured();
    }

    public OcrParseResult<OcrResultDto> parseTimetable(MultipartFile image, Integer year) throws IOException {
        return geminiOcrClient.parseTimetable(image, year);
    }

    public OcrApplyResultDto applyEntries(OcrApplyRequestDto request) {
        List<OcrFailure> failures = new ArrayList<>();
        int savedCount = 0;

        List<OcrResultDto> entries = request.entries();
        for (int i = 0; i < entries.size(); i++) {
            OcrResultDto entry = entries.get(i);
            Optional<String> error = validateEntry(entry);
            if (error.isPresent()) {
                failures.add(toFailure(entry, error.get(), i));
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
                failures.add(toFailure(entry, reason, i));
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
        if (entry.isAnnouncement()) {
            req.setStageName(TimetableEntry.ANNOUNCEMENT_SENTINEL);
        } else {
            req.setStageName(entry.stage() != null ? entry.stage().trim() : "");
        }
        req.setArtistName(entry.artist() != null ? entry.artist().trim() : "");
        req.setFestivalDate(LocalDate.parse(entry.date()));
        req.setStartTime(LocalTime.parse(entry.startTime()));
        req.setEndTime(LocalTime.parse(entry.endTime()));
        return req;
    }

    private OcrFailure toFailure(OcrResultDto entry, String reason, int index) {
        return new OcrFailure(
                index,
                Objects.requireNonNullElse(entry.artist(), "—"),
                Objects.requireNonNullElse(entry.stage(), "—"),
                Objects.requireNonNullElse(reason, "알 수 없는 오류"));
    }
}
