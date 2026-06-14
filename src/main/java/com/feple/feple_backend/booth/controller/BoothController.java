package com.feple.feple_backend.booth.controller;

import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.service.BoothService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "부스", description = "페스티벌 부스 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/festivals/{festivalId}/booths")
public class BoothController {

    private final BoothService boothService;

    @GetMapping
    public ResponseEntity<List<BoothResponseDto>> getBooths(@PathVariable Long festivalId) {
        return ResponseEntity.ok(boothService.getBooths(festivalId));
    }
}
