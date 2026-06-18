package com.feple.feple_backend.artist.song.controller;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "노래 신청", description = "아티스트 노래 신청·목록 조회")
@RestController
@RequestMapping("/artists/{artistId}/song-requests")
@RequiredArgsConstructor
public class SongRequestController {

    private final SongRequestService songRequestService;

    @PostMapping
    public ResponseEntity<SongRequestResponseDto> submit(
            @PathVariable Long artistId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubmitSongRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(songRequestService.submit(artistId, userId, dto));
    }

    @GetMapping
    public ResponseEntity<List<SongRequestResponseDto>> getMyRequests(
            @PathVariable Long artistId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(songRequestService.getMyRequests(artistId, userId));
    }
}
