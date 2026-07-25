package com.feple.feple_backend.festival.suggestion.controller;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import com.feple.feple_backend.festival.suggestion.dto.SubmitFestivalSuggestionDto;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "페스티벌 제안", description = "등록되지 않은 페스티벌 제안")
@RestController
@RequestMapping("/festival-suggestions")
@RequiredArgsConstructor
public class FestivalSuggestionController {

    private final FestivalSuggestionService festivalSuggestionService;

    @PostMapping
    public ResponseEntity<FestivalSuggestionResponseDto> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitFestivalSuggestionDto dto) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(festivalSuggestionService.submit(userId, dto));
    }
}
