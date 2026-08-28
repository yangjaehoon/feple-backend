package com.feple.feple_backend.artist.song.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
    @Index(name = "idx_song_request_status", columnList = "status")
})
public class SongRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String songTitle;

    private String youtubeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SongRequestStatus status;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = SongRequestStatus.PENDING;
        }
    }

    public Long getArtistId() { return artist.getId(); }
    public String getArtistName() { return artist.getName(); }
    public String getArtistNameEn() { return artist.getNameEn() != null ? artist.getNameEn() : ""; }
    public boolean isPending() { return status == SongRequestStatus.PENDING; }

    public void approve() {
        this.status = SongRequestStatus.APPROVED;
    }

    public void reject() {
        this.status = SongRequestStatus.REJECTED;
    }

    public void updateYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }
}
