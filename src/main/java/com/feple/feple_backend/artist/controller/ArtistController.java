package com.feple.feple_backend.artist.controller;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "아티스트", description = "아티스트 조회 및 스케줄")
@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;
    private final ArtistScheduleService artistScheduleService;

    @GetMapping
    public List<ArtistResponseDto> getArtists() {
        return artistService.getAllArtists();
    }

    @GetMapping("/{id}")
    public ArtistResponseDto getArtistById(@PathVariable Long id){
        return artistService.getArtistById(id);
    }

    @GetMapping("/{id}/schedule")
    public List<ArtistScheduleResponseDto> getArtistSchedule(@PathVariable Long id) {
        return artistScheduleService.getArtistSchedule(id);
    }
}
