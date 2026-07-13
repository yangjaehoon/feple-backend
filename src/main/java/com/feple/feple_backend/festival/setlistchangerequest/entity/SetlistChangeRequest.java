package com.feple.feple_backend.festival.setlistchangerequest.entity;

import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "lineup_change_requests") // DB 테이블명은 변경하지 않음 (운영 공유 DB, 마이그레이션 범위 밖)
public class SetlistChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "festival_id", nullable = false)
    private Long festivalId;

    @Column(name = "artist_festival_id", nullable = false)
    private Long artistFestivalId;

    @Column(name = "artist_name", nullable = false, length = 100)
    private String artistName;

    @Column(name = "festival_title", nullable = false, length = 200)
    private String festivalTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SetlistChangeRequestStatus status = SetlistChangeRequestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getUserId() { return user.getId(); }

    public static SetlistChangeRequest of(User user, Long festivalId, Long artistFestivalId,
                                         String artistName, String festivalTitle, String message) {
        SetlistChangeRequest req = new SetlistChangeRequest();
        req.user = user;
        req.festivalId = festivalId;
        req.artistFestivalId = artistFestivalId;
        req.artistName = artistName;
        req.festivalTitle = festivalTitle;
        req.message = message;
        return req;
    }

    public void resolve() {
        this.status = SetlistChangeRequestStatus.PROCESSED;
    }
}
