package com.feple.feple_backend.artistfollow.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "artist_follow",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "artist_id"})
        }
)
public class ArtistFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;


    public static ArtistFollow of(User user, Artist artist) {
        ArtistFollow af = new ArtistFollow();
        af.user = user;
        af.artist = artist;
        return af;
    }
}