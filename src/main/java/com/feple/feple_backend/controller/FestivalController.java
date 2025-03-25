package com.feple.feple_backend.controller;

import com.feple.feple_backend.dto.festival.FestivalRequestDto;
import com.feple.feple_backend.dto.festival.FestivalResponseDto;
import com.feple.feple_backend.service.FestivalService;
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
    public ResponseEntity<List<FestivalResponseDto>> getAllFestivals() {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals();
        return ResponseEntity.ok(festivals);
    }
}
