package com.feple.feple_backend.artist.song.controller;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/artists/{artistId}/song-requests")
@RequiredArgsConstructor
public class SongRequestController {

    private final SongRequestService songRequestService;

    @PostMapping
    public ResponseEntity<SongRequestResponseDto> submit(
            @PathVariable Long artistId,
            Authentication authentication,
            @RequestBody SubmitSongRequestDto dto) {
        Long userId = (Long) authentication.getPrincipal();
        SongRequestResponseDto response = songRequestService.submit(artistId, userId, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<SongRequestResponseDto>> getMyRequests(
            @PathVariable Long artistId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<SongRequestResponseDto> response = songRequestService.getMyRequests(artistId, userId);
        return ResponseEntity.ok(response);
    }
}
