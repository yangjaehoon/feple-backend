package com.feple.feple_backend.artistfestival.controller;

import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "아티스트-페스티벌", description = "페스티벌 출연 아티스트 목록 조회")
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

}
