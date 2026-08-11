package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.service.TimetableService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TimetableOcrService {

    private final GeminiOcrClient geminiOcrClient;
    private final TimetableService timetableService;

    public boolean isConfigured() {
        return geminiOcrClient.isConfigured();
    }

    public OcrParseResult<TimetableOcrResultDto> parseTimetable(MultipartFile image, Integer year) throws IOException {
        return geminiOcrClient.parseTimetable(image, year);
    }

    public TimetableOcrApplyResultDto applyEntries(TimetableOcrApplyRequestDto request) {
        List<TimetableOcrFailure> failures = new ArrayList<>();

        // 엔트리마다 festival을 재조회하지 않도록 한 번만 조회해 재사용한다(60~100개 엔트리 포스터
        // 적용 시 N+1 방지). festivalId는 관리자가 선택한 단일 값으로 요청 전체에서 동일하다.
        Festival festival = timetableService.getFestivalOrThrow(request.festivalId());
        List<TimetableOcrResultDto> entries = request.entries();

        // 형식 검증을 먼저 걸러내고, 통과한 항목만 모아 한 번에 배치 생성한다(항목마다 스테이지 조회·
        // 라인업 역동기화 쿼리를 반복하던 것을 TimetableService.createEntriesBatch로 일괄 처리).
        // validIndices는 원래 인덱스를 그대로 보존해, 값이 같은 두 항목을 indexOf로 혼동하지 않게 한다.
        List<TimetableOcrResultDto> validEntries = new ArrayList<>();
        List<Integer> validIndices = new ArrayList<>();
        List<TimetableEntryRequestDto> requests = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            TimetableOcrResultDto entry = entries.get(i);
            Optional<String> error = validateEntry(entry);
            if (error.isPresent()) {
                failures.add(toFailure(entry, error.get(), i));
                continue;
            }
            validEntries.add(entry);
            validIndices.add(i);
            requests.add(toTimetableRequest(entry));
        }

        List<TimetableService.BatchCreateResult> results = timetableService.createEntriesBatch(festival, requests);
        int savedCount = 0;
        for (int i = 0; i < results.size(); i++) {
            TimetableService.BatchCreateResult result = results.get(i);
            if (result.error() == null) {
                savedCount++;
                continue;
            }
            RuntimeException e = result.error();
            // 사용자에게 드러낼 수 있는 검증 오류만 메시지 전달, 내부 예외는 고정 문구 사용
            String reason = (e instanceof IllegalArgumentException || e instanceof NoSuchElementException)
                    ? e.getMessage()
                    : "처리 중 오류 발생";
            failures.add(toFailure(validEntries.get(i), reason, validIndices.get(i)));
        }
        return new TimetableOcrApplyResultDto(savedCount, failures.size(), failures);
    }

    private Optional<String> validateEntry(TimetableOcrResultDto entry) {
        if (entry.date() == null || entry.date().isBlank())
            return Optional.of("날짜 누락");
        if (entry.startTime() == null || entry.endTime() == null)
            return Optional.of("시작/종료 시간 누락");
        try {
            LocalDate.parse(entry.date());
        } catch (DateTimeParseException ex) {
            return Optional.of("날짜 형식 오류: " + entry.date());
        }
        try {
            LocalTime.parse(entry.startTime());
            LocalTime.parse(entry.endTime());
        } catch (DateTimeParseException ex) {
            return Optional.of("시간 형식 오류: " + entry.startTime() + "~" + entry.endTime());
        }
        return Optional.empty();
    }

    private TimetableEntryRequestDto toTimetableRequest(TimetableOcrResultDto entry) {
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

    private TimetableOcrFailure toFailure(TimetableOcrResultDto entry, String reason, int index) {
        return new TimetableOcrFailure(
                index,
                Objects.requireNonNullElse(entry.artist(), "—"),
                Objects.requireNonNullElse(entry.stage(), "—"),
                Objects.requireNonNullElse(reason, "알 수 없는 오류"));
    }
}
