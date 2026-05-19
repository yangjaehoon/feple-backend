package com.feple.feple_backend.artist.song.entity;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistFestivalSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_festival_id", nullable = false)
    private ArtistFestival artistFestival;

    @Builder
    public ArtistFestivalSong(Song song, ArtistFestival artistFestival) {
        this.song = song;
        this.artistFestival = artistFestival;
    }
}
