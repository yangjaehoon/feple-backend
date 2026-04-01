package com.feple.feple_backend.booth.controller;

import com.feple.feple_backend.booth.dto.BoothResponseDto;
import com.feple.feple_backend.booth.service.BoothService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
