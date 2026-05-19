package com.feple.feple_backend.artist.song.controller;

import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping("/{artistId}/songs")
    public List<SongResponseDto> getSongs(@PathVariable Long artistId) {
        return songService.getSongsByArtistId(artistId);
    }
}
