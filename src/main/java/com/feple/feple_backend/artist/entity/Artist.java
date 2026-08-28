package com.feple.feple_backend.artist.entity;

import com.feple.feple_backend.global.MusicGenre;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // 페스티벌 라인업처럼 여러 Artist를 순회하며 genres에 접근하는 화면에서 N+1을 막기 위해
    // 배치로 묶어 조회한다 (findByFestivalIdOrderByLineupOrderAsc가 artist는 JOIN FETCH하지만
    // genres는 List 타입 @ElementCollection이라 같은 쿼리에 함께 fetch join하면 중복 행이 생긴다)
    @ElementCollection(targetClass = MusicGenre.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "artist_genres", joinColumns = @JoinColumn(name = "artist_id"))
    @Column(name = "genres", length = 20)
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.BatchSize(size = 100)
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

    private LocalDateTime deletedAt;

    public boolean isDeleted() { return deletedAt != null; }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

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
