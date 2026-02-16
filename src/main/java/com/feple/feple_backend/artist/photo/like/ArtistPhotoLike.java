package com.feple.feple_backend.artist.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_photo_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"artist_photo_id", "user_id"})
})
public class ArtistPhotoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_photo_id", nullable = false)
    private Long artistPhotoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ArtistPhotoLike(Long artistPhotoId, Long userId) {
        this.artistPhotoId = artistPhotoId;
        this.userId = userId;
    }
}