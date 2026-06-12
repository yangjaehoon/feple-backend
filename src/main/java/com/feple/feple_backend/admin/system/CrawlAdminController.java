package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.OcrApplyRequest;
import com.feple.feple_backend.admin.OcrApplyResultDto;
import com.feple.feple_backend.admin.OcrResultDto;
import com.feple.feple_backend.admin.OcrService;
import com.feple.feple_backend.admin.ScrapedFestivalDto;
import com.feple.feple_backend.admin.ScrapedFestivalMapper;
import com.feple.feple_backend.admin.ScraperApplyRequest;
import com.feple.feple_backend.admin.WebScraperService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final StageService stageService;
    private final ArtistFestivalService artistFestivalService;

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
            return ResponseEntity.badRequest().body(Map.of("error", "URL을 입력해주세요."));
        }
        try {
            ScrapedFestivalDto result = webScraperService.scrape(url.trim(), source);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("스크래핑 실패: {}", url, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "페이지를 가져오는데 실패했습니다. URL을 확인하거나 직접 입력해주세요."));
        }
    }

    @PostMapping("/scrape/apply")
    @ResponseBody
    public ResponseEntity<?> applyScrape(@RequestBody ScraperApplyRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "제목을 입력해주세요."));
        }
        if (req.startDate() == null || req.startDate().isBlank()
                || req.endDate() == null || req.endDate().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "시작일과 종료일을 입력해주세요."));
        }
        try {
            java.time.LocalDate.parse(req.startDate());
            java.time.LocalDate.parse(req.endDate());
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)"));
        }
        try {
            Long festivalId = festivalService.createFestival(ScrapedFestivalMapper.toFestivalRequestDto(req));
            return ResponseEntity.ok(Map.of("festivalId", festivalId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("스크래핑 결과 페스티벌 등록 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "페스티벌 등록에 실패했습니다."));
        }
    }

    // ── OCR ──────────────────────────────────────────────

    @PostMapping("/ocr")
    @ResponseBody
    public ResponseEntity<?> parseOcr(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미지를 업로드해주세요."));
        }
        if (!ocrService.isConfigured()) {
            return ResponseEntity.status(503).body(Map.of("error", "Gemini API 키가 설정되지 않았습니다. application-local.yaml에 app.gemini.api-key를 설정하세요."));
        }
        try {
            List<OcrResultDto> results = ocrService.parseTimeTable(image);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("OCR 파싱 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "이미지 파싱에 실패했습니다. 다시 시도해주세요."));
        }
    }

    @PostMapping("/ocr/apply")
    @ResponseBody
    public ResponseEntity<?> applyOcr(@RequestBody OcrApplyRequest request) {
        if (request.festivalId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "페스티벌을 선택해주세요."));
        }
        if (request.entries() == null || request.entries().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "저장할 항목이 없습니다."));
        }
        try {
            OcrApplyResultDto result = ocrService.applyEntries(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR 타임테이블 적용 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "타임테이블 저장에 실패했습니다."));
        }
    }

    // ── 드롭다운 데이터 ──────────────────────────────────

    @GetMapping("/festivals")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFestivals() {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals(null, null, null, true);
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
        List<String> stageNames = stageService.getStages(festivalId)
                .stream()
                .map(Stage::getName)
                .toList();
        return ResponseEntity.ok(stageNames);
    }

    @GetMapping("/festivals/{festivalId}/artists")
    @ResponseBody
    public ResponseEntity<List<String>> getArtists(@PathVariable Long festivalId) {
        List<String> artistNames = artistFestivalService.getArtistFestivals(festivalId)
                .stream()
                .map(ArtistFestivalResponse::getArtistName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return ResponseEntity.ok(artistNames);
    }
}
