package com.feple.feple_backend.admin;

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
    private final FestivalService festivalService;
    private final StageService stageService;

    @GetMapping
    public String crawlDashboard() {
        return "admin/crawl-dashboard";
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
        if (request.festivalId() == null || request.festivalDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "페스티벌과 날짜를 선택해주세요."));
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
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals(null, null, true);
        List<Map<String, Object>> result = festivals.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",        f.getId());
            m.put("title",     f.getTitle());
            m.put("startDate", f.getStartDate() != null ? f.getStartDate().toString() : null);
            m.put("endDate",   f.getEndDate()   != null ? f.getEndDate().toString()   : null);
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
}
