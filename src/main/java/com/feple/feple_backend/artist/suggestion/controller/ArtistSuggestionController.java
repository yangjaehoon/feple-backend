package com.feple.feple_backend.artist.suggestion.controller;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "아티스트 제안", description = "등록되지 않은 아티스트 제안")
@RestController
@RequestMapping("/artist-suggestions")
@RequiredArgsConstructor
public class ArtistSuggestionController {

    private final ArtistSuggestionService artistSuggestionService;

    @PostMapping
    public ResponseEntity<ArtistSuggestionResponseDto> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitArtistSuggestionDto dto) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(artistSuggestionService.submit(userId, dto));
    }
}
