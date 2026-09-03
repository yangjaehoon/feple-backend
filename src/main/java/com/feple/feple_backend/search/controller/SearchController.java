package com.feple.feple_backend.search.controller;

import com.feple.feple_backend.search.dto.SearchResultDto;
import com.feple.feple_backend.search.dto.SuggestionDto;
import com.feple.feple_backend.search.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "검색", description = "페스티벌·아티스트·게시글 통합 검색")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResultDto> search(
            @RequestParam @NotBlank @Size(max = 100) String keyword,
            @AuthenticationPrincipal Long userId) {
        // 비로그인 게스트(userId == null)의 검색어는 집계 로그에 남기지 않는다.
        return ResponseEntity.ok(searchService.search(keyword, userId != null));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<SuggestionDto>> getSuggestions(
            @RequestParam @NotBlank @Size(max = 100) String keyword) {
        return ResponseEntity.ok(searchService.getSuggestions(keyword));
    }
}
