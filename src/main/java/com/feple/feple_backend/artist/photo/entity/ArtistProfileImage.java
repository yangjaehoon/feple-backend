package com.feple.feple_backend.artist.photo.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artist_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArtistProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    private LocalDateTime uploadAt;

    @OneToMany(mappedBy = "artistProfileImage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistProfileImageLike> likes = new ArrayList<>();

    public int getLikeCount(){
        return likes.size();
    }
}
