package com.feple.feple_backend.festival.controller;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
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
    public ResponseEntity<Long> createFestival(@RequestBody FestivalRequestDto dto) {
        Long id = festivalService.createFestival(dto);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<List<FestivalResponseDto>> getAllFestivals() {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals();
        return ResponseEntity.ok(festivals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FestivalDetailResponseDto> getFestival(@PathVariable Long id) {
        FestivalDetailResponseDto detail = festivalService.getFestivalDetail(id);
        return ResponseEntity.ok(detail);
    }
}
