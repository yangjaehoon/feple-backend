package com.feple.feple_backend.timetable.controller;

import com.feple.feple_backend.timetable.dto.TimetableEntryResponse;
import com.feple.feple_backend.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "타임테이블", description = "페스티벌 공연 타임테이블 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/festivals/{festivalId}/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<List<TimetableEntryResponse>> getEntries(@PathVariable Long festivalId) {
        return ResponseEntity.ok(timetableService.getEntries(festivalId));
    }

}
