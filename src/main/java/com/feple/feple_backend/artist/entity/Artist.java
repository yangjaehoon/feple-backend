package com.feple.feple_backend.artist.entity;

import com.feple.feple_backend.global.MusicGenre;
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

    // 관리자 동시 편집 시 lost update 방지
    @Version
    private Long version;

    private String name;

    private String nameEn;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "artist_aliases", joinColumns = @JoinColumn(name = "artist_id"))
    @Column(name = "alias", length = 200)
    @Builder.Default
    private List<String> aliases = new ArrayList<>();

    @ElementCollection(targetClass = MusicGenre.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "artist_genres", joinColumns = @JoinColumn(name = "artist_id"))
    @Column(name = "genres", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<MusicGenre> genres = new ArrayList<>();

    private String profileImageKey;

    @Column(nullable = false)
    @Builder.Default
    private int followerCount = 0;

    @Builder.Default
    private int weeklyScore = 0;

    private LocalDateTime rankUpdatedAt;

    public String getAliasesDisplay() {
        return (aliases == null || aliases.isEmpty()) ? null : String.join(", ", aliases);
    }

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

    public void update(String name, String nameEn, List<MusicGenre> genres, List<String> aliases) {
        this.name = name;
        this.nameEn = nameEn;
        this.genres = genres != null ? new ArrayList<>(genres) : new ArrayList<>();
        this.aliases.clear();
        if (aliases != null) this.aliases.addAll(aliases);
    }

    public void updateProfileImage(String newKey) {
        this.profileImageKey = newKey;
    }

    public void updateNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

}
