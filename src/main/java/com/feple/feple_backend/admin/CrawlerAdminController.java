package com.feple.feple_backend.admin;

import com.feple.feple_backend.crawler.FestivalCrawlerService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/crawler")
public class CrawlerAdminController {

    private final FestivalCrawlerService crawlerService;
    private final FestivalService festivalService;

    /** 크롤링 대기 목록 */
    @GetMapping
    public String crawlerList(Model model) {
        List<FestivalResponseDto> drafts = festivalService.getDraftFestivals();
        model.addAttribute("drafts", drafts);
        return "admin/crawler-list";
    }

    /** 수동 크롤링 실행 */
    @PostMapping("/run")
    public String runCrawl(RedirectAttributes ra) {
        int count = crawlerService.crawlAll();
        ra.addFlashAttribute("successMessage", "크롤링 완료 — 신규 " + count + "개 수집");
        return "redirect:/admin/crawler";
    }

    /** DRAFT → PUBLISHED */
    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        festivalService.publishDraft(id);
        ra.addFlashAttribute("successMessage", "앱에 공개되었습니다.");
        return "redirect:/admin/crawler";
    }

    /** DRAFT 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        festivalService.deleteDraft(id);
        ra.addFlashAttribute("successMessage", "삭제되었습니다.");
        return "redirect:/admin/crawler";
    }
}
