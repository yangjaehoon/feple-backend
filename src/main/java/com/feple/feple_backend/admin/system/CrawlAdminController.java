package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.ocr.OcrService;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 크롤 대시보드("/admin/crawl") 진입 페이지 + 페이지 안 여러 위젯(스크래핑/OCR/
 * 라인업 OCR)이 공통으로 쓰는 조회 전용 데이터(페스티벌·스테이지·아티스트
 * 드롭다운, Gemini 사용량)를 제공한다. 실제 스크래핑/OCR 액션은
 * {@link WebScrapeAdminController}/{@link TimetableOcrAdminController}/
 * {@link ArtistLineupOcrAdminController}로 분리되어 있다.
 */
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/crawl")
public class CrawlAdminController {

    private final OcrService ocrService;
    private final FestivalService festivalService;
    private final StageService stageService;
    private final ArtistFestivalService artistFestivalService;

    @GetMapping
    public String crawlDashboard() {
        return "admin/system/crawl";
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
}
