package com.feple.feple_backend.timetable.controller;

import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "타임테이블", description = "페스티벌 공연 타임테이블 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/festivals/{festivalId}/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<List<TimetableEntryResponseDto>> getEntries(@PathVariable Long festivalId) {
        return ResponseEntity.ok(timetableService.getEntries(festivalId));
    }

}
