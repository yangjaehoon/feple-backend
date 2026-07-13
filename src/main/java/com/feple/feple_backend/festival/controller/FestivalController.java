package com.feple.feple_backend.festival.controller;

import com.feple.feple_backend.artist.song.dto.FestivalSetlistEntryDto;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.global.MusicGenre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.festival.service.WeatherService;
import com.feple.feple_backend.festival.lineupchangerequest.service.LineupChangeRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final LineupChangeRequestService lineupChangeRequestService;

    @GetMapping
    public List<FestivalResponseDto> getAllFestivals(
            @RequestParam(required = false) List<MusicGenre> genres,
            @RequestParam(required = false) List<Region> regions,
            @RequestParam(required = false) List<AgeRestriction> ageRestrictions,
            @RequestParam(defaultValue = "false") boolean includeEnded,
            @RequestParam(required = false) String sort) {
        return festivalService.getAllFestivals(new FestivalFilterCriteria(genres, regions, ageRestrictions, includeEnded, sort));
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

    record SetlistChangeRequestBody(
        @NotNull Long artistFestivalId,
        @NotBlank @Size(max = 500) String message
    ) {}

    @PostMapping("/{id}/setlist-requests")
    public ResponseEntity<Void> submitSetlistRequest(
            @PathVariable Long id,
            @Valid @RequestBody SetlistChangeRequestBody body,
            @AuthenticationPrincipal Long userId) {
        lineupChangeRequestService.submit(userId, id, body.artistFestivalId(), body.message());
        return ResponseEntity.noContent().build();
    }
}
