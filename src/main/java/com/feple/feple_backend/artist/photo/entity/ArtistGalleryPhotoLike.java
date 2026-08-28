package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "artist_photo_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "artist_photo_id", "user_id" })
})
public class ArtistGalleryPhotoLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_photo_id", nullable = false)
    private ArtistGalleryPhoto photo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ArtistGalleryPhotoLike(ArtistGalleryPhoto photo, User user) {
        this.photo = photo;
        this.user = user;
    }
}
