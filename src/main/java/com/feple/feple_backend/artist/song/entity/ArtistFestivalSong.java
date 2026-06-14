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
@Table(name = "artist_festival_song",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_afs_song_artist_festival", columnNames = {"song_id", "artist_festival_id"})
    }
)
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

    public Long getSongId() { return song.getId(); }
    public Long getArtistFestivalId() { return artistFestival.getId(); }

    @Builder
    public ArtistFestivalSong(Song song, ArtistFestival artistFestival) {
        this.song = song;
        this.artistFestival = artistFestival;
    }
}
