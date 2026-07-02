package com.feple.feple_backend.artist.song.controller;

import com.feple.feple_backend.artist.song.dto.SongFestivalDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.service.SongService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "노래", description = "아티스트 노래 목록 및 세트리스트")
@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping("/{artistId}/songs")
    public List<SongResponseDto> getSongs(@PathVariable Long artistId) {
        return songService.getSongsByArtistId(artistId);
    }

    @GetMapping("/{artistId}/songs/{songId}/festivals")
    public List<SongFestivalDto> getSongFestivals(@PathVariable Long songId) {
        return songService.getSongFestivals(songId);
    }
}
