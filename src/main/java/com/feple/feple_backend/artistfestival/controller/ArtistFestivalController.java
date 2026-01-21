package com.feple.feple_backend.artistfestival.controller;

import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/festivals/{festivalId}/artists")
public class ArtistFestivalController {

    private final ArtistFestivalService artistFestivalService;

    @GetMapping
    public ResponseEntity<List<ArtistFestivalResponse>> getArtistFestivals(
            @PathVariable Long festivalId
    ) {
        List<ArtistFestivalResponse> responses =
                artistFestivalService.getArtistFestivals(festivalId);
        return ResponseEntity.ok(responses);
    }

    // 페스티벌에 아티스트 추가
    @PostMapping
    public ResponseEntity<Long> addArtistToFestival(
            @PathVariable Long festivalId,
            @RequestBody ArtistFestivalCreateRequest request
    ) {
        Long id = artistFestivalService.addArtistToFestival(festivalId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

}
