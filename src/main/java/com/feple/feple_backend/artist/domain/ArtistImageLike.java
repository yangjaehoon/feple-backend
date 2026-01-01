package com.feple.feple_backend.artist.domain;

import com.feple.feple_backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "artist_image_like",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "artist_image_id"})
        }
)
public class ArtistImageLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_image_id")
    private ArtistImage artistImage;
}
