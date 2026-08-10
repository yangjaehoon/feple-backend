package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.admin.AdminErrorResponses;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/** 크롤 대시보드의 라인업 포스터 OCR 파싱/적용 + 미매칭 아티스트 제안 관리 전담 컨트롤러. */
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.CRAWL)
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/crawl/ocr/lineup")
public class ArtistLineupOcrAdminController {

    private final ArtistLineupOcrService ocrService;
    private final AdminLogService adminLogService;

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> parseLineupOcr(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) return AdminErrorResponses.badRequest("이미지를 업로드해주세요.");
        if (AdminErrorResponses.isNotImage(image)) return AdminErrorResponses.badRequest("이미지 파일만 업로드할 수 있습니다.");
        if (!ocrService.isConfigured()) return AdminErrorResponses.geminiNotConfigured();
        try {
            OcrParseResult<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(image);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("라인업 OCR 파싱 실패", e);
            return AdminErrorResponses.serverError("이미지 파싱에 실패했습니다. 다시 시도해주세요.");
        }
    }

    @PostMapping("/apply")
    @ResponseBody
    public ResponseEntity<?> applyLineupOcr(@RequestBody LineupOcrApplyRequestDto request) {
        if (request.festivalId() == null)                            return AdminErrorResponses.badRequest("페스티벌을 선택해주세요.");
        if (request.artistIds() == null || request.artistIds().isEmpty()) return AdminErrorResponses.badRequest("등록할 아티스트가 없습니다.");
        try {
            LineupApplyResult result = ocrService.applyArtistLineup(request);
            adminLogService.log(AdminAction.LINEUP_OCR_APPLY, "FESTIVAL", request.festivalId(),
                    "added=" + result.added() + " duplicates=" + result.duplicates() + " failed=" + result.failed());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("라인업 OCR 적용 실패: festivalId={}", request.festivalId(), e);
            return AdminErrorResponses.serverError("아티스트 등록에 실패했습니다.");
        }
    }

    @GetMapping("/suggestions")
    @ResponseBody
    public ResponseEntity<List<UnmatchedArtistSuggestionDto>> getSuggestions() {
        return ResponseEntity.ok(ocrService.getSuggestions());
    }

    @DeleteMapping("/suggestions/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteSuggestion(@PathVariable Long id) {
        ocrService.deleteSuggestion(id);
        adminLogService.log(AdminAction.UNMATCHED_SUGGESTION_DELETE, "UNMATCHED_SUGGESTION", id, null);
        return ResponseEntity.noContent().build();
    }
}
