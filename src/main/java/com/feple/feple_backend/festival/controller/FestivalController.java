package com.feple.feple_backend.festival.controller;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/festivals")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;

    @PostMapping
    public ResponseEntity<Void> createFestival(@RequestBody FestivalRequestDto dto) {
        festivalService.createFestival(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FestivalDetailResponseDto>> getAllFestivals() {
        List<FestivalDetailResponseDto> festivals = festivalService.getAllFestivals();
        return ResponseEntity.ok(festivals);
    }
}
