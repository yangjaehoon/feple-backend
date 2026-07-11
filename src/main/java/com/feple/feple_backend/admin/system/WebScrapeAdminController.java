package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalDto;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalMapper;
import com.feple.feple_backend.admin.scraper.ScraperApplyRequestDto;
import com.feple.feple_backend.admin.scraper.WebScraperService;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** 크롤 대시보드의 웹 스크래핑(URL로 페스티벌 정보 자동 추출) 기능 전담 컨트롤러. */
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/crawl")
public class WebScrapeAdminController {

    private final WebScraperService webScraperService;
    private final FestivalAdminService festivalAdminService;
    private final AdminLogService adminLogService;

    @PostMapping("/scrape")
    @ResponseBody
    public ResponseEntity<?> scrape(@RequestBody Map<String, String> body) {
        String url    = body.get("url");
        String source = body.getOrDefault("source", "direct");
        if (url == null || url.isBlank()) {
            return CrawlAdminResponses.badRequest("URL을 입력해주세요.");
        }
        try {
            ScrapedFestivalDto result = webScraperService.scrape(url.trim(), source);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return CrawlAdminResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("스크래핑 실패: {}", url, e);
            return CrawlAdminResponses.serverError("페이지를 가져오는데 실패했습니다. URL을 확인하거나 직접 입력해주세요.");
        }
    }

    @PostMapping("/scrape/apply")
    @ResponseBody
    public ResponseEntity<?> applyScrape(@RequestBody ScraperApplyRequestDto req) {
        String validationError = validate(req);
        if (validationError != null) return CrawlAdminResponses.badRequest(validationError);
        try {
            Long festivalId = festivalAdminService.createFestival(ScrapedFestivalMapper.toFestivalRequestDto(req));
            adminLogService.log(AdminAction.FESTIVAL_SCRAPE_CREATE, "FESTIVAL", festivalId, req.title());
            return ResponseEntity.ok(Map.of("festivalId", festivalId));
        } catch (IllegalArgumentException e) {
            return CrawlAdminResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("스크래핑 결과 페스티벌 등록 실패", e);
            return CrawlAdminResponses.serverError("페스티벌 등록에 실패했습니다.");
        }
    }

    private static String validate(ScraperApplyRequestDto req) {
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
}
