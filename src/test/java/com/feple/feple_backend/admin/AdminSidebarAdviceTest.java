package com.feple.feple_backend.admin;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class AdminSidebarAdviceTest {

    @Mock AdminSidebarCountService sidebarCountService;
    @Mock Model model;

    @InjectMocks AdminSidebarAdvice advice;

    @Test
    void 정상_조회시_카운트를_모델에_설정() {
        given(sidebarCountService.getCounts()).willReturn(new AdminSidebarCountService.Counts(1, 2, 3, 4, 5, 6));

        advice.sidebarCounts(model);

        verify(model).addAttribute("sidebarReportCount", 1L);
        verify(model).addAttribute("sidebarCertCount", 2L);
        verify(model).addAttribute("sidebarSongRequestCount", 3L);
        verify(model).addAttribute("sidebarArtistSuggestionCount", 4L);
        verify(model).addAttribute("sidebarFestivalSuggestionCount", 5L);
        verify(model).addAttribute("sidebarSetlistRequestCount", 6L);
    }

    @Test
    void 조회실패시_0으로_폴백() {
        willThrow(new RuntimeException("조회 실패")).given(sidebarCountService).getCounts();

        advice.sidebarCounts(model);

        verify(model).addAttribute("sidebarReportCount", 0L);
        verify(model).addAttribute("sidebarCertCount", 0L);
        verify(model).addAttribute("sidebarSongRequestCount", 0L);
        verify(model).addAttribute("sidebarArtistSuggestionCount", 0L);
        verify(model).addAttribute("sidebarFestivalSuggestionCount", 0L);
        verify(model).addAttribute("sidebarSetlistRequestCount", 0L);
    }
}
