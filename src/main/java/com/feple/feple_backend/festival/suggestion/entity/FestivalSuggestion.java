package com.feple.feple_backend.festival.suggestion.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
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
public class FestivalSuggestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String festivalName;

    private String note;

    @Column(length = 500)
    private String processNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FestivalSuggestionStatus status;

    private LocalDateTime processedAt;

    private Long approvedFestivalId;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = FestivalSuggestionStatus.PENDING;
    }

    public boolean isPending() { return status == FestivalSuggestionStatus.PENDING; }

    public void approve(Long festivalId) {
        this.status = FestivalSuggestionStatus.APPROVED;
        this.approvedFestivalId = festivalId;
        this.processedAt = LocalDateTime.now();
    }

    public void dismiss(String processNote) {
        this.status = FestivalSuggestionStatus.DISMISSED;
        this.processNote = processNote;
        this.processedAt = LocalDateTime.now();
    }
}
