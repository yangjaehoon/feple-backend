package com.feple.feple_backend.admin;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final GeminiOcrClient geminiOcrClient;
    private final TimetableService timetableService;

    public boolean isConfigured() {
        return geminiOcrClient.isConfigured();
    }

    public List<OcrResultDto> parseTimeTable(MultipartFile image) throws IOException {
        return geminiOcrClient.parseTimeTable(image);
    }

    public OcrApplyResultDto applyEntries(OcrApplyRequest request) {
        List<Map<String, String>> failures = new ArrayList<>();
        int savedCount = 0;

        for (OcrResultDto entry : request.entries()) {
            Optional<String> error = validateEntry(entry);
            if (error.isPresent()) {
                failures.add(buildFailureMap(entry, error.get()));
                continue;
            }
            try {
                timetableService.createEntry(request.festivalId(), toTimetableRequest(entry));
                savedCount++;
            } catch (Exception e) {
                failures.add(buildFailureMap(entry, e.getMessage()));
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

    private TimetableEntryRequest toTimetableRequest(OcrResultDto entry) {
        TimetableEntryRequest req = new TimetableEntryRequest();
        req.setStageName(entry.stage()  != null ? entry.stage().trim()  : "");
        req.setArtistName(entry.artist() != null ? entry.artist().trim() : "");
        req.setFestivalDate(LocalDate.parse(entry.date()));
        req.setStartTime(LocalTime.parse(entry.startTime()));
        req.setEndTime(LocalTime.parse(entry.endTime()));
        return req;
    }

    private Map<String, String> buildFailureMap(OcrResultDto entry, String reason) {
        Map<String, String> map = new HashMap<>();
        map.put("artist", entry.artist() != null ? entry.artist() : "—");
        map.put("stage",  entry.stage()  != null ? entry.stage()  : "—");
        map.put("reason", reason != null ? reason : "알 수 없는 오류");
        return map;
    }
}
