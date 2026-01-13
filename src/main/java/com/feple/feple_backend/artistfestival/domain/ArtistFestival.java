package com.feple.feple_backend.artistfestival.domain;


import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.festival.domain.Festival;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//artist와 festival 사이에 중간 엔티티
@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class ArtistFestival {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne
    @JoinColumn(name = "festival_id")
    private Festival festival;

    private Integer lineupOrder;
    private String stageName;

    @Builder
    public ArtistFestival(Artist artist, Festival festival,
                          Integer lineupOrder, String stageName) {
        this.artist = artist;
        this.festival = festival;
        this.lineupOrder = lineupOrder;
        this.stageName = stageName;
    }
}
