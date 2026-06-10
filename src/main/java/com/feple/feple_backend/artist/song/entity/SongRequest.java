package com.feple.feple_backend.artist.song.entity;

import com.feple.feple_backend.artist.entity.Artist;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(indexes = {
    @Index(name = "idx_song_request_status", columnList = "status")
})
public class SongRequest {

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

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SongRequestStatus.PENDING;
        }
    }

    public Long getArtistId() { return artist.getId(); }
    public String getArtistName() { return artist.getName(); }

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
