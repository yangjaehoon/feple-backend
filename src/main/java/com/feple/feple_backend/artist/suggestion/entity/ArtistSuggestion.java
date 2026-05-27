package com.feple.feple_backend.artist.suggestion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArtistSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String artistName;

    private String note;

    @Column(length = 500)
    private String processNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtistSuggestionStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = ArtistSuggestionStatus.PENDING;
    }

    public void dismiss(String processNote) {
        this.status = ArtistSuggestionStatus.DISMISSED;
        this.processNote = processNote;
        this.processedAt = LocalDateTime.now();
    }
}
