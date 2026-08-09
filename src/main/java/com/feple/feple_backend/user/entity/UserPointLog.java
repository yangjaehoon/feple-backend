package com.feple.feple_backend.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static UserPointLog of(User user, PointEntry entry) {
        UserPointLog log = new UserPointLog();
        log.user = user;
        log.delta = entry.delta();
        log.reason = entry.reason();
        log.refId = entry.refId();
        return log;
    }

    /** 관리자가 사유를 직접 입력해 수동 지급하는 경우 전용 — 자동 적립(of)과 달리 note에 사유 원문을 남긴다. */
    public static UserPointLog ofAdminGrant(User user, int delta, String note) {
        UserPointLog log = new UserPointLog();
        log.user = user;
        log.delta = delta;
        log.reason = PointReason.ADMIN_GRANTED;
        log.note = note;
        return log;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getUserNickname() {
        return user.getNickname();
    }
}
