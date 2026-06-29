package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/artists")
public class FestivalArtistAdminController {

    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final ArtistFestivalService artistFestivalService;

    @GetMapping("/new")
    @Transactional(readOnly = true)
    public String addArtistForm(@PathVariable Long festivalId, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(festivalId);
        List<ArtistResponseDto> allArtists = artistService.getAllArtistsSortedByName();

        Set<Long> participatingIds = artistFestivalService.getArtistFestivals(festivalId)
                .stream()
                .map(ArtistFestivalResponseDto::getArtistId)
                .collect(Collectors.toSet());

        model.addAttribute("festival", festival);
        model.addAttribute("artists", allArtists);
        model.addAttribute("participatingIds", participatingIds);
        model.addAttribute("request", new ArtistFestivalCreateRequestDto());
        return "admin/festival/artist-form";
    }

    @PostMapping
    public String addArtistToFestival(
            @PathVariable Long festivalId,
            @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
            RedirectAttributes ra) {
        if (artistIds == null || artistIds.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "아티스트를 한 명 이상 선택해주세요.");
            return "redirect:/admin/festivals/" + festivalId + "/artists/new";
        }
        ArtistAddResult result = processAdditions(festivalId, artistIds);
        applyFlashMessage(result, ra);
        return AdminFestivalRedirects.detail(festivalId);
    }

    private ArtistAddResult processAdditions(Long festivalId, List<Long> artistIds) {
        int added = 0, duplicates = 0, errors = 0;
        for (Long artistId : artistIds) {
            try {
                ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
                req.setArtistId(artistId);
                artistFestivalService.addArtistToFestival(festivalId, req);
                added++;
            } catch (ConflictException ignored) {
                duplicates++;
            } catch (Exception e) {
                log.error("아티스트 추가 실패: festivalId={}, artistId={}", festivalId, artistId, e);
                errors++;
            }
        }
        return new ArtistAddResult(added, duplicates, errors);
    }

    private void applyFlashMessage(ArtistAddResult result, RedirectAttributes ra) {
        if (result.added() == 0 && result.errors() == 0) {
            ra.addFlashAttribute("errorMessage", "선택한 아티스트가 이미 모두 참여 중입니다.");
        } else if (result.added() == 0) {
            ra.addFlashAttribute("errorMessage", "아티스트 추가에 실패했습니다. 다시 시도해주세요.");
        } else {
            StringBuilder msg = new StringBuilder(result.added() + "명의 아티스트가 추가되었습니다.");
            if (result.duplicates() > 0) msg.append(" (").append(result.duplicates()).append("명은 이미 참여 중)");
            if (result.errors() > 0) msg.append(" (").append(result.errors()).append("명 추가 실패)");
            ra.addFlashAttribute("successMessage", msg.toString());
        }
    }

    @GetMapping("/list")
    @ResponseBody
    public List<ArtistFestivalResponseDto> getFestivalArtists(@PathVariable Long festivalId) {
        return artistFestivalService.getArtistFestivals(festivalId);
    }

    @PostMapping("/{artistFestivalId}/edit")
    public String updateLineup(@PathVariable Long festivalId,
                               @PathVariable Long artistFestivalId,
                               @RequestParam(required = false) String stageName,
                               @RequestParam(required = false) String performanceDate,
                               RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> artistFestivalService.updateArtistFestival(festivalId, artistFestivalId, stageName, parseDate(performanceDate)),
                "라인업이 수정되었습니다.",
                e -> log.error("라인업 수정 실패: festivalId={}, afId={}", festivalId, artistFestivalId, e),
                "라인업 수정 중 오류가 발생했습니다.",
                ra);
        return AdminFestivalRedirects.artists(festivalId);
    }

    @PostMapping("/batch-edit")
    public String batchUpdateLineup(@PathVariable Long festivalId,
                                    @RequestParam("afIds") List<Long> afIds,
                                    @RequestParam("performanceDates") List<String> performanceDates,
                                    @RequestParam("stageNames") List<String> stageNames,
                                    RedirectAttributes ra) {
        int errorCount = 0;
        for (int i = 0; i < afIds.size(); i++) {
            try {
                artistFestivalService.updateArtistFestival(
                        festivalId, afIds.get(i),
                        safeGet(stageNames, i),
                        parseDate(safeGet(performanceDates, i)));
            } catch (Exception e) {
                log.warn("batchUpdateLineup 실패: festivalId={}, afId={}", festivalId, afIds.get(i), e);
                errorCount++;
            }
        }
        if (errorCount > 0) {
            ra.addFlashAttribute("errorMessage", errorCount + "건 수정 실패. 항목을 확인해 주세요.");
        } else {
            ra.addFlashAttribute("successMessage", "라인업이 일괄 수정되었습니다.");
        }
        return AdminFestivalRedirects.artists(festivalId);
    }

    private static String safeGet(List<String> list, int i) {
        return i < list.size() ? list.get(i) : null;
    }

    private static LocalDate parseDate(String dateStr) {
        return (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr) : null;
    }

    @PostMapping("/{artistFestivalId}/delete")
    public String removeArtistFromFestival(
            @PathVariable Long festivalId,
            @PathVariable Long artistFestivalId,
            RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> artistFestivalService.removeArtistFromFestival(festivalId, artistFestivalId),
                null,
                e -> log.error("아티스트 제거 실패: festivalId={}, afId={}", festivalId, artistFestivalId, e),
                "아티스트 제거 중 오류가 발생했습니다.",
                ra);
        return AdminFestivalRedirects.artists(festivalId);
    }

    private record ArtistAddResult(int added, int duplicates, int errors) {}
}
