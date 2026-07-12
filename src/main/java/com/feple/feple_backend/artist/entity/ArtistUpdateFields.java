package com.feple.feple_backend.artist.entity;

import com.feple.feple_backend.global.MusicGenre;

import java.util.List;

public record ArtistUpdateFields(String name, String nameEn, List<MusicGenre> genres, List<String> aliases) {
}
