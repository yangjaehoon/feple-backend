package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_photo_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "artist_photo_id", "user_id" })
})
public class ArtistGalleryPhotoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_photo_id", nullable = false)
    private ArtistGalleryPhoto photo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ArtistGalleryPhotoLike(ArtistGalleryPhoto photo, User user) {
        this.photo = photo;
        this.user = user;
    }
}
