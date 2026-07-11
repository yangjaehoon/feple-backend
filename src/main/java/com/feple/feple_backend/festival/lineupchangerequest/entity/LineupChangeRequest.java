package com.feple.feple_backend.festival.lineupchangerequest.entity;

import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "lineup_change_requests")
public class LineupChangeRequest {

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
    private LineupChangeRequestStatus status = LineupChangeRequestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getUserId() { return user.getId(); }

    public static LineupChangeRequest of(User user, Long festivalId, Long artistFestivalId,
                                         String artistName, String festivalTitle, String message) {
        LineupChangeRequest req = new LineupChangeRequest();
        req.user = user;
        req.festivalId = festivalId;
        req.artistFestivalId = artistFestivalId;
        req.artistName = artistName;
        req.festivalTitle = festivalTitle;
        req.message = message;
        return req;
    }

    public void resolve() {
        this.status = LineupChangeRequestStatus.PROCESSED;
    }
}
