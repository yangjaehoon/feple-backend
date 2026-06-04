package com.feple.feple_backend.admin;

import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            String artistLabel = entry.artist() != null ? entry.artist() : "—";
            try {
                if (entry.date() == null || entry.date().isBlank()) {
                    failures.add(Map.of("artist", artistLabel, "stage", entry.stage() != null ? entry.stage() : "—", "reason", "날짜 누락"));
                    continue;
                }
                if (entry.startTime() == null || entry.endTime() == null) {
                    failures.add(Map.of("artist", artistLabel, "stage", entry.stage() != null ? entry.stage() : "—", "reason", "시작/종료 시간 누락"));
                    continue;
                }
                java.time.LocalDate festivalDate;
                try {
                    festivalDate = java.time.LocalDate.parse(entry.date());
                } catch (java.time.format.DateTimeParseException ex) {
                    failures.add(Map.of("artist", artistLabel, "stage", entry.stage() != null ? entry.stage() : "—", "reason", "날짜 형식 오류: " + entry.date()));
                    continue;
                }
                TimetableEntryRequest req = new TimetableEntryRequest();
                req.setStageName(entry.stage() != null ? entry.stage().trim() : "");
                req.setArtistName(entry.artist() != null ? entry.artist().trim() : "");
                req.setFestivalDate(festivalDate);
                req.setStartTime(LocalTime.parse(entry.startTime()));
                req.setEndTime(LocalTime.parse(entry.endTime()));
                timetableService.createEntry(request.festivalId(), req);
                savedCount++;
            } catch (Exception e) {
                Map<String, String> failure = new HashMap<>();
                failure.put("artist", artistLabel);
                failure.put("stage",  entry.stage()  != null ? entry.stage()  : "—");
                failure.put("reason", e.getMessage());
                failures.add(failure);
            }
        }
        return new OcrApplyResultDto(savedCount, failures.size(), failures);
    }
}
