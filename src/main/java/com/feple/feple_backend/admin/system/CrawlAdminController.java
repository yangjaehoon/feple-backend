package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.ocr.ArtistLineupOcrResult;
import com.feple.feple_backend.admin.ocr.LineupOcrApplyRequestDto;
import com.feple.feple_backend.admin.ocr.LineupApplyResult;
import com.feple.feple_backend.admin.ocr.OcrApplyRequestDto;
import com.feple.feple_backend.admin.ocr.OcrApplyResultDto;
import com.feple.feple_backend.admin.ocr.OcrParseResult;
import com.feple.feple_backend.admin.ocr.OcrResultDto;
import com.feple.feple_backend.admin.ocr.OcrService;
import com.feple.feple_backend.admin.ocr.UnmatchedArtistSuggestionDto;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalDto;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalMapper;
import com.feple.feple_backend.admin.scraper.ScraperApplyRequestDto;
import com.feple.feple_backend.admin.scraper.WebScraperService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/crawl")
public class CrawlAdminController {

    private final OcrService ocrService;
    private final WebScraperService webScraperService;
    private final FestivalService festivalService;
    private final FestivalAdminService festivalAdminService;
    private final StageService stageService;
    private final ArtistFestivalService artistFestivalService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String crawlDashboard() {
        return "admin/system/crawl";
    }

    // ── 웹 스크래핑 ─────────────────────────────────────

    @PostMapping("/scrape")
    @ResponseBody
    public ResponseEntity<?> scrape(@RequestBody Map<String, String> body) {
        String url    = body.get("url");
        String source = body.getOrDefault("source", "direct");
        if (url == null || url.isBlank()) {
            return badRequest("URL을 입력해주세요.");
        }
        try {
            ScrapedFestivalDto result = webScraperService.scrape(url.trim(), source);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("스크래핑 실패: {}", url, e);
            return serverError("페이지를 가져오는데 실패했습니다. URL을 확인하거나 직접 입력해주세요.");
        }
    }

    @PostMapping("/scrape/apply")
    @ResponseBody
    public ResponseEntity<?> applyScrape(@RequestBody ScraperApplyRequestDto req) {
        String validationError = validateScraperApplyRequestDto(req);
        if (validationError != null) return badRequest(validationError);
        try {
            Long festivalId = festivalAdminService.createFestival(ScrapedFestivalMapper.toFestivalRequestDto(req));
            adminLogService.log(AdminAction.FESTIVAL_SCRAPE_CREATE, "FESTIVAL", festivalId, req.title());
            return ResponseEntity.ok(Map.of("festivalId", festivalId));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("스크래핑 결과 페스티벌 등록 실패", e);
            return serverError("페스티벌 등록에 실패했습니다.");
        }
    }

    // ── OCR ──────────────────────────────────────────────

    @PostMapping("/ocr")
    @ResponseBody
    public ResponseEntity<?> parseOcr(@RequestParam("image") MultipartFile image,
                                       @RequestParam(value = "year", required = false) Integer year) {
        if (image.isEmpty()) return badRequest("이미지를 업로드해주세요.");
        if (!ocrService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE,
                            "Gemini API 키가 설정되지 않았습니다. application-local.yaml에 app.gemini.api-key를 설정하세요.",
                            ErrorCode.SERVICE_UNAVAILABLE));
        }
        try {
            OcrParseResult<OcrResultDto> results = ocrService.parseTimetable(image, year);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("OCR 파싱 실패", e);
            return serverError("이미지 파싱에 실패했습니다. 다시 시도해주세요.");
        }
    }

    @PostMapping("/ocr/apply")
    @ResponseBody
    public ResponseEntity<?> applyOcr(@RequestBody OcrApplyRequestDto request) {
        if (request.festivalId() == null)                    return badRequest("페스티벌을 선택해주세요.");
        if (request.entries() == null || request.entries().isEmpty()) return badRequest("저장할 항목이 없습니다.");
        try {
            OcrApplyResultDto result = ocrService.applyEntries(request);
            adminLogService.log(AdminAction.TIMETABLE_OCR_APPLY, "FESTIVAL", request.festivalId(),
                    "saved=" + result.savedCount() + " failed=" + result.failedCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR 타임테이블 적용 실패", e);
            return serverError("타임테이블 저장에 실패했습니다.");
        }
    }

    @PostMapping("/ocr/lineup")
    @ResponseBody
    public ResponseEntity<?> parseLineupOcr(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) return badRequest("이미지를 업로드해주세요.");
        if (!ocrService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE,
                            "Gemini API 키가 설정되지 않았습니다. application-local.yaml에 app.gemini.api-key를 설정하세요.",
                            ErrorCode.SERVICE_UNAVAILABLE));
        }
        try {
            OcrParseResult<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(image);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("라인업 OCR 파싱 실패", e);
            return serverError("이미지 파싱에 실패했습니다. 다시 시도해주세요.");
        }
    }

    @PostMapping("/ocr/lineup/apply")
    @ResponseBody
    public ResponseEntity<?> applyLineupOcr(@RequestBody LineupOcrApplyRequestDto request) {
        if (request.festivalId() == null)                            return badRequest("페스티벌을 선택해주세요.");
        if (request.artistIds() == null || request.artistIds().isEmpty()) return badRequest("등록할 아티스트가 없습니다.");
        try {
            LineupApplyResult result = ocrService.applyArtistLineup(request);
            adminLogService.log(AdminAction.LINEUP_OCR_APPLY, "FESTIVAL", request.festivalId(),
                    "added=" + result.added() + " duplicates=" + result.duplicates());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("라인업 OCR 적용 실패: festivalId={}", request.festivalId(), e);
            return serverError("아티스트 등록에 실패했습니다.");
        }
    }

    @GetMapping("/ocr/lineup/suggestions")
    @ResponseBody
    public ResponseEntity<List<UnmatchedArtistSuggestionDto>> getSuggestions() {
        return ResponseEntity.ok(ocrService.getSuggestions());
    }

    @DeleteMapping("/ocr/lineup/suggestions/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteSuggestion(@PathVariable Long id) {
        ocrService.deleteSuggestion(id);
        adminLogService.log(AdminAction.UNMATCHED_SUGGESTION_DELETE, "UNMATCHED_SUGGESTION", id, null);
        return ResponseEntity.noContent().build();
    }

    // ── Gemini 사용량 ────────────────────────────────────

    @GetMapping("/quota")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQuota() {
        Map<String, Object> result = new HashMap<>();
        result.put("used",  ocrService.getTodayUsage());
        result.put("limit", ocrService.getDailyLimit());
        result.put("remaining", Math.max(0, ocrService.getDailyLimit() - ocrService.getTodayUsage()));
        return ResponseEntity.ok(result);
    }

    // ── 드롭다운 데이터 ──────────────────────────────────

    @GetMapping("/festivals")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFestivals() {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals(FestivalFilterCriteria.forAdmin());
        List<Map<String, Object>> result = festivals.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",        f.getId());
            m.put("title",     f.getTitle());
            m.put("startDate", f.getStartDateIso());
            m.put("endDate",   f.getEndDateIso());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/festivals/{festivalId}/stages")
    @ResponseBody
    public ResponseEntity<List<String>> getStages(@PathVariable Long festivalId) {
        return ResponseEntity.ok(stageService.getStages(festivalId).stream()
                .map(Stage::getName).toList());
    }

    @GetMapping("/festivals/{festivalId}/artists")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtists(@PathVariable Long festivalId) {
        return ResponseEntity.ok(
                artistFestivalService.getArtistFestivalsWithEnName(festivalId).stream()
                        .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                                (String) a.get("name"), (String) b.get("name")))
                        .toList());
    }

    // ── 내부 헬퍼 ────────────────────────────────────────

    private static String validateScraperApplyRequestDto(ScraperApplyRequestDto req) {
        if (req.title() == null || req.title().isBlank())
            return "제목을 입력해주세요.";
        if (req.startDate() == null || req.startDate().isBlank()
                || req.endDate() == null || req.endDate().isBlank())
            return "시작일과 종료일을 입력해주세요.";
        try {
            LocalDate.parse(req.startDate());
            LocalDate.parse(req.endDate());
        } catch (DateTimeParseException e) {
            return "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)";
        }
        return null;
    }

    private static ResponseEntity<ErrorResponse> badRequest(String error) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, error, ErrorCode.ILLEGAL_ARGUMENT));
    }

    private static ResponseEntity<ErrorResponse> serverError(String error) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, error, ErrorCode.SERVER_ERROR));
    }
}
