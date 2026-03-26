package com.feple.feple_backend.timetable.controller;

import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponse;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/festivals/{festivalId}/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    public ResponseEntity<List<TimetableEntryResponse>> getEntries(@PathVariable Long festivalId) {
        return ResponseEntity.ok(timetableService.getEntries(festivalId));
    }

    @PostMapping
    public ResponseEntity<TimetableEntryResponse> createEntry(
            @PathVariable Long festivalId,
            @RequestBody TimetableEntryRequest req) {
        return ResponseEntity.ok(timetableService.createEntry(festivalId, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable Long festivalId,
            @PathVariable Long id) {
        timetableService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
}
