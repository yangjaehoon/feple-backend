package com.feple.feple_backend.artistfestival.domain;


import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.festival.domain.Festival;
import jakarta.persistence.*;

//artist와 festival 사이에 중간 엔티티
@Entity
class ArtistFestival {
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

}
