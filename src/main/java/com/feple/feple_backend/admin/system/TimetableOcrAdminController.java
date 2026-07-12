package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.ocr.OcrApplyRequestDto;
import com.feple.feple_backend.admin.ocr.OcrApplyResultDto;
import com.feple.feple_backend.admin.ocr.OcrParseResult;
import com.feple.feple_backend.admin.ocr.OcrResultDto;
import com.feple.feple_backend.admin.ocr.TimetableOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/** 크롤 대시보드의 타임테이블 포스터 OCR 파싱/적용 기능 전담 컨트롤러. */
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/crawl/ocr")
public class TimetableOcrAdminController {

    private final TimetableOcrService ocrService;
    private final AdminLogService adminLogService;

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> parseOcr(@RequestParam("image") MultipartFile image,
                                       @RequestParam(value = "year", required = false) Integer year) {
        if (image.isEmpty()) return CrawlAdminResponses.badRequest("이미지를 업로드해주세요.");
        if (!ocrService.isConfigured()) return CrawlAdminResponses.geminiNotConfigured();
        try {
            OcrParseResult<OcrResultDto> results = ocrService.parseTimetable(image, year);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("OCR 파싱 실패", e);
            return CrawlAdminResponses.serverError("이미지 파싱에 실패했습니다. 다시 시도해주세요.");
        }
    }

    @PostMapping("/apply")
    @ResponseBody
    public ResponseEntity<?> applyOcr(@RequestBody OcrApplyRequestDto request) {
        if (request.festivalId() == null)                    return CrawlAdminResponses.badRequest("페스티벌을 선택해주세요.");
        if (request.entries() == null || request.entries().isEmpty()) return CrawlAdminResponses.badRequest("저장할 항목이 없습니다.");
        try {
            OcrApplyResultDto result = ocrService.applyEntries(request);
            adminLogService.log(AdminAction.TIMETABLE_OCR_APPLY, "FESTIVAL", request.festivalId(),
                    "saved=" + result.savedCount() + " failed=" + result.failedCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR 타임테이블 적용 실패", e);
            return CrawlAdminResponses.serverError("타임테이블 저장에 실패했습니다.");
        }
    }
}
