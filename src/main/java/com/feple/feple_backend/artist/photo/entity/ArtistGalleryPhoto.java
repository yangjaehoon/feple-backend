package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_photos", indexes = {
        @Index(name = "idx_gallery_photo_artist_id", columnList = "artist_id"),
        @Index(name = "idx_gallery_photo_uploader_id", columnList = "uploader_user_id")
})
public class ArtistGalleryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_user_id", nullable = false)
    private User uploader;

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

    @Column(name = "likecount", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int likeCount = 0;

    public ArtistGalleryPhoto(Artist artist, User uploader, String s3Key, String contentType, String title,
            String description) {
        this.artist = artist;
        this.uploader = uploader;
        this.s3Key = s3Key;
        this.contentType = contentType;
        this.title = title;
        this.description = description;
    }

    public void updateTitleAndDescription(String title, String description) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}
