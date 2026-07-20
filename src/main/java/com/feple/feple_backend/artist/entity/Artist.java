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

    @Column(length = 500)
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

    public void updateWeeklyScore(int score) {
        this.weeklyScore = score;
        this.rankUpdatedAt = LocalDateTime.now();
    }

    public void update(ArtistUpdateFields fields) {
        this.name = fields.name();
        this.nameEn = fields.nameEn();
        this.genres = fields.genres() != null ? new ArrayList<>(fields.genres()) : new ArrayList<>();
        this.aliases.clear();
        if (fields.aliases() != null) this.aliases.addAll(fields.aliases());
    }

    public void updateProfileImage(String newKey) {
        this.profileImageKey = newKey;
    }

    public void updateNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

}
