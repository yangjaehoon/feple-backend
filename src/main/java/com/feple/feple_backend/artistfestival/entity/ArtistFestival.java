package com.feple.feple_backend.artistfestival.entity;


import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

//artist와 festival 사이에 중간 엔티티
@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(name = "artist_festival",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_af_artist_festival", columnNames = {"artist_id", "festival_id"})
    },
    indexes = {
        @Index(name = "idx_af_artist_id", columnList = "artist_id"),
        @Index(name = "idx_af_festival_id", columnList = "festival_id")
    }
)
public class ArtistFestival {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id")
    private Festival festival;

    private Integer lineupOrder;
    private String stageName;
    private LocalDate performanceDate;

    @Builder
    public ArtistFestival(Artist artist, Festival festival,
                          Integer lineupOrder, String stageName, LocalDate performanceDate) {
        this.artist = artist;
        this.festival = festival;
        this.lineupOrder = lineupOrder;
        this.stageName = stageName;
        this.performanceDate = performanceDate;
    }

    public Long getArtistId() { return artist.getId(); }
    public String getArtistName()   { return artist.getName(); }
    public String getArtistNameEn() { return artist.getNameEn(); }
    public String getArtistGenreDisplayName() {
        if (artist.getGenres() == null || artist.getGenres().isEmpty()) return null;
        return artist.getGenres().stream()
                .map(com.feple.feple_backend.artist.entity.ArtistGenre::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }
    public String getArtistProfileImageKey() { return artist.getProfileImageKey(); }
    public Long getFestivalId() { return festival.getId(); }

    public void updateLineup(String stageName, LocalDate performanceDate) {
        this.stageName = stageName;
        this.performanceDate = performanceDate;
    }
}
