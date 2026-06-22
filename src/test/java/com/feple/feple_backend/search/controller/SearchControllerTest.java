package com.feple.feple_backend.search.controller;

import com.feple.feple_backend.search.dto.SearchResultDto;
import com.feple.feple_backend.search.dto.SuggestionDto;
import com.feple.feple_backend.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock SearchService searchService;

    @InjectMocks SearchController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // GlobalExceptionHandler를 추가하지 않아야 Spring MVC 기본 예외 처리(400)가 동작한다.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 검색_성공() throws Exception {
        SearchResultDto resultDto = mock(SearchResultDto.class);
        given(searchService.search("봄")).willReturn(resultDto);

        mockMvc.perform(get("/search").param("keyword", "봄"))
                .andExpect(status().isOk());
    }

    @Test
    void 검색_키워드_없으면_400() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 추천_검색어_성공() throws Exception {
        SuggestionDto suggestionDto = mock(SuggestionDto.class);
        given(searchService.getSuggestions("봄")).willReturn(List.of(suggestionDto));

        mockMvc.perform(get("/search/suggestions").param("keyword", "봄"))
                .andExpect(status().isOk());
    }

    @Test
    void 추천_검색어_키워드_없으면_400() throws Exception {
        mockMvc.perform(get("/search/suggestions"))
                .andExpect(status().isBadRequest());
    }
}
