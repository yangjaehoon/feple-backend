package com.feple.feple_backend.artist.suggestion.controller;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artist-suggestions")
@RequiredArgsConstructor
public class ArtistSuggestionController {

    private final ArtistSuggestionService artistSuggestionService;

    @PostMapping
    public ResponseEntity<ArtistSuggestionResponseDto> submit(
            Authentication authentication,
            @RequestBody SubmitArtistSuggestionDto dto) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(artistSuggestionService.submit(userId, dto));
    }
}
