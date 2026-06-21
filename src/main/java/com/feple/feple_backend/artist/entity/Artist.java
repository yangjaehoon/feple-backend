package com.feple.feple_backend.artist.entity;

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
@Table(indexes = {
    @Index(name = "idx_artist_follower_count", columnList = "follower_count DESC"),
    @Index(name = "idx_artist_weekly_score", columnList = "weekly_score DESC, id ASC")
})
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String nameEn;

    @Convert(converter = ArtistGenreConverter.class)
    @Column(name = "genre")
    @Builder.Default
    private List<ArtistGenre> genres = new ArrayList<>();

    private String profileImageKey;

    @Column(nullable = false)
    @Builder.Default
    private int followerCount = 0;

    @Builder.Default
    private int weeklyScore = 0;

    private LocalDateTime rankUpdatedAt;

    public void incrementFollowerCount() {
        this.followerCount++;
    }

    public void decrementFollowerCount() {
        if (this.followerCount > 0) this.followerCount--;
    }

    public void updateWeeklyScore(int score) {
        this.weeklyScore = score;
        this.rankUpdatedAt = LocalDateTime.now();
    }

    public void update(String name, String nameEn, List<ArtistGenre> genres) {
        this.name = name;
        this.nameEn = nameEn;
        this.genres = genres != null ? genres : new ArrayList<>();
    }

    public void updateProfileImage(String newKey) {
        this.profileImageKey = newKey;
    }

    public void updateNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

}
