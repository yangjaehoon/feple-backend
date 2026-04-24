package com.feple.feple_backend.festival.controller;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/festivals")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;
    private final FestivalLikeService festivalLikeService;

    @GetMapping
    public List<FestivalResponseDto> getAllFestivals(
            @RequestParam(required = false) List<Genre> genres,
            @RequestParam(required = false) List<Region> regions,
            @RequestParam(defaultValue = "false") boolean includeEnded) {
        return festivalService.getAllFestivals(genres, regions, includeEnded);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FestivalDetailResponseDto> getFestival(@PathVariable Long id) {
        FestivalDetailResponseDto detail = festivalService.getFestivalDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(festivalLikeService.toggleLike(id, userId));
    }

    @GetMapping("/{id}/liked")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(festivalLikeService.isLiked(id, userId));
    }
}
