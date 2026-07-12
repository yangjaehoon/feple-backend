package com.feple.feple_backend.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_point_log", indexes = {
    @Index(name = "idx_user_point_log_user_id", columnList = "user_id")
})
public class UserPointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointReason reason;

    private Long refId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected UserPointLog() {}

    public static UserPointLog of(User user, PointEntry entry) {
        UserPointLog log = new UserPointLog();
        log.user = user;
        log.delta = entry.delta();
        log.reason = entry.reason();
        log.refId = entry.refId();
        return log;
    }
}
