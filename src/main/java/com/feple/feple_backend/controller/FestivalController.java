package com.feple.feple_backend.controller;

import com.feple.feple_backend.dto.festival.FestivalRequestDto;
import com.feple.feple_backend.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
