package com.feple.feple_backend.domain.artist;

import com.feple.feple_backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArtistImage {

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

    @OneToMany(mappedBy = "artistImage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistImageLike> likes = new ArrayList<>();

    public int getLikeCount(){
        return likes.size();
    }
}
