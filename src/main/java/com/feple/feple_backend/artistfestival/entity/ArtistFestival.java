package com.feple.feple_backend.artistfestival.entity;


import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//artist와 festival 사이에 중간 엔티티
@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(name = "artist_festival", indexes = {
    @Index(name = "idx_af_artist_id", columnList = "artist_id"),
    @Index(name = "idx_af_festival_id", columnList = "festival_id")
})
public class ArtistFestival {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
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

    public void updateLineup(Integer lineupOrder, String stageName) {
        this.lineupOrder = lineupOrder;
        this.stageName = stageName;
    }
}
