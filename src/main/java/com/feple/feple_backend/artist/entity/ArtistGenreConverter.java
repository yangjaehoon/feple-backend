package com.feple.feple_backend.artist.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Converter
public class ArtistGenreConverter implements AttributeConverter<List<ArtistGenre>, String> {

    @Override
    public String convertToDatabaseColumn(List<ArtistGenre> genres) {
        if (genres == null || genres.isEmpty()) return null;
        return genres.stream().map(ArtistGenre::name).collect(Collectors.joining(","));
    }

    @Override
    public List<ArtistGenre> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::parse)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArtistGenre parse(String s) {
        try {
            return ArtistGenre.valueOf(s);
        } catch (IllegalArgumentException e) {
            return switch (s) {
                case "밴드", "Band"    -> ArtistGenre.BAND;
                case "힙합", "Hip-hop" -> ArtistGenre.HIP_HOP;
                case "인디", "Indie"   -> ArtistGenre.INDIE;
                case "발라드", "Ballad" -> ArtistGenre.BALLAD;
                case "R&B", "알앤비"   -> ArtistGenre.RNB;
                case "댄스"            -> ArtistGenre.DANCE;
                case "아이돌"           -> ArtistGenre.IDOL;
                default                -> null;
            };
        }
    }
}
