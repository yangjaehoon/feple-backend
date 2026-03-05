package com.feple.feple_backend.artist.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private ArtistGenre genre;

    private String profileImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private int followerCount = 0;

    public void update(String name, ArtistGenre genre, String profileImageUrl) {
        this.name = name;
        this.genre = genre;
        this.profileImageUrl = profileImageUrl;
    }

    public void increaseFollowCount() {
        this.followerCount++;
    }

    public void decreaseFollowCount() {
        if (this.followerCount > 0) this.followerCount--;
    }
}
