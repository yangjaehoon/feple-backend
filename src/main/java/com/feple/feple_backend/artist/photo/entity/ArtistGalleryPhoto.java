package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_photos", indexes = {
        @Index(name = "idx_gallery_photo_artist_id", columnList = "artist_id"),
        @Index(name = "idx_gallery_photo_uploader_id", columnList = "uploader_user_id")
})
public class ArtistGalleryPhoto extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_user_id", nullable = false)
    private User uploader;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "like_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int likeCount = 0;

    @Column(name = "is_anonymous", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean anonymous = false;

    public ArtistGalleryPhoto(Artist artist, User uploader, String s3Key, String contentType, String title,
            String description, boolean anonymous) {
        this.artist = artist;
        this.uploader = uploader;
        this.s3Key = s3Key;
        this.contentType = contentType;
        this.title = title;
        this.description = description;
        this.anonymous = anonymous;
    }

    public void updateTitleAndDescription(String title, String description) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
    }

    public Long getUploaderId() { return uploader.getId(); }
    public String getUploaderNickname() { return uploader.getNickname(); }
    public String getArtistName() { return artist.getName(); }
}
