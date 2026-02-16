package com.feple.feple_backend.artist.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_photos")
public class ArtistPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long artistId;

    @Column(nullable = false)
    private Long uploaderUserId;

    @Column(nullable = false, length = 500)
    private String s3Key;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ArtistPhoto(Long artistId, Long uploaderUserId, String s3Key, String contentType) {
        this.artistId = artistId;
        this.uploaderUserId = uploaderUserId;
        this.s3Key = s3Key;
        this.contentType = contentType;
    }
}