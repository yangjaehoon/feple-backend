package com.feple.feple_backend.artist.controller;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping
    public List<ArtistResponseDto> getArtists(){
        return artistService.getAllArtists();
    };

    @GetMapping("/{id}")
    public ArtistResponseDto getArtistById(@PathVariable Long id){
        return artistService.getArtistById(id);
    }
}
