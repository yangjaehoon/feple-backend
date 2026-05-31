package com.feple.feple_backend.artist.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ArtistGenreConverter implements AttributeConverter<ArtistGenre, String> {

    @Override
    public String convertToDatabaseColumn(ArtistGenre genre) {
        return genre == null ? null : genre.name();
    }

    @Override
    public ArtistGenre convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return ArtistGenre.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            return switch (dbData) {
                case "밴드", "Band"           -> ArtistGenre.BAND;
                case "힙합", "Hip-hop"        -> ArtistGenre.HIP_HOP;
                case "인디", "Indie"          -> ArtistGenre.INDIE;
                case "발라드", "Ballad"       -> ArtistGenre.BALLAD;
                case "R&B", "알앤비"          -> ArtistGenre.RNB;
                default                       -> null;
            };
        }
    }
}
