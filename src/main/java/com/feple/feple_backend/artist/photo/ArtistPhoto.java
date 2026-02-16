package com.feple.feple_backend.artist.entity;

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

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 500, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int likecount = 0;

    public ArtistPhoto(Long artistId, Long uploaderUserId, String s3Key, String contentType, String title, String description) {
        this.artistId = artistId;
        this.uploaderUserId = uploaderUserId;
        this.s3Key = s3Key;
        this.contentType = contentType;
        this.title = title;
        this.description = description;
    }
}