package com.feple.feple_backend.festival.controller;

import com.feple.feple_backend.artist.song.dto.FestivalSetlistEntryDto;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.festival.service.WeatherService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "페스티벌", description = "페스티벌 조회, 좋아요, 참석 여부, 날씨")
@RestController
@RequestMapping("/festivals")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;
    private final FestivalLikeService festivalLikeService;
    private final FestivalAttendanceService festivalAttendanceService;
    private final WeatherService weatherService;
    private final SongService songService;
    private final SongAdminService songAdminService;

    @GetMapping
    public List<FestivalResponseDto> getAllFestivals(
            @RequestParam(required = false) List<Genre> genres,
            @RequestParam(required = false) List<Region> regions,
            @RequestParam(required = false) List<AgeRestriction> ageRestrictions,
            @RequestParam(defaultValue = "false") boolean includeEnded) {
        return festivalService.getAllFestivals(genres, regions, ageRestrictions, includeEnded);
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
        if (userId == null) return ResponseEntity.ok(false);
        return ResponseEntity.ok(festivalLikeService.isLiked(id, userId));
    }

    @PostMapping("/{id}/attending")
    public ResponseEntity<Boolean> toggleAttending(@PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(festivalAttendanceService.toggleAttending(id, userId));
    }

    @GetMapping("/{id}/attending")
    public ResponseEntity<Boolean> isAttending(@PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) return ResponseEntity.ok(false);
        return ResponseEntity.ok(festivalAttendanceService.isAttending(id, userId));
    }

    @GetMapping("/{id}/weather")
    public ResponseEntity<WeatherDto> getWeather(@PathVariable Long id) {
        return weatherService.getByFestivalId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/setlist")
    public List<FestivalSetlistEntryDto> getSetlist(@PathVariable Long id) {
        return songService.getFestivalSetlist(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/artists/{artistFestivalId}/setlist")
    public ResponseEntity<Void> updateSetlist(@PathVariable Long id,
                                              @PathVariable Long artistFestivalId,
                                              @RequestBody(required = false) Set<Long> songIds) {
        songAdminService.updateSetlist(id, artistFestivalId, songIds != null ? songIds : Set.of());
        return ResponseEntity.ok().build();
    }
}
