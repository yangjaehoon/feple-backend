package com.feple.feple_backend.admin.ocr;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "unmatched_artist_suggestion")
@Getter
@NoArgsConstructor
public class UnmatchedArtistSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false)
    private int mentionCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UnmatchedArtistSuggestion of(String name) {
        UnmatchedArtistSuggestion s = new UnmatchedArtistSuggestion();
        s.name = name;
        s.mentionCount = 1;
        s.createdAt = LocalDateTime.now();
        s.updatedAt = LocalDateTime.now();
        return s;
    }

    public void incrementMentionCount() {
        this.mentionCount++;
        this.updatedAt = LocalDateTime.now();
    }
}
