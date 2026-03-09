package com.feple.feple_backend.artist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    //private String profileImageUrl;
    private String profileImageKey;

    @Column(nullable = false)
    @Builder.Default
    private int followerCount = 0;

    @Builder.Default
    private int weeklyScore = 0;

    private LocalDateTime rankUpdatedAt;

    public void updateWeeklyScore(int score) {
        this.weeklyScore = score;
        this.rankUpdatedAt = LocalDateTime.now();
    }

    public void update(String name, ArtistGenre genre, String profileImageUrl) {
        this.name = name;
        this.genre = genre;
        //this.profileImageUrl = profileImageUrl;
        this.profileImageKey = profileImageUrl;
    }

    public void increaseFollowCount() {
        this.followerCount++;
    }

    public void decreaseFollowCount() {
        if (this.followerCount > 0) this.followerCount--;
    }
}
