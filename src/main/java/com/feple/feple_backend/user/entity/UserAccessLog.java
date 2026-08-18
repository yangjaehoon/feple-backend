package com.feple.feple_backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 사용자별 하루 첫 접속 시각만 남기는 로그(유저당 하루 1행) — user_id, access_date에 유니크 제약. */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_access_log", indexes = {
        @Index(name = "idx_user_access_log_access_date", columnList = "access_date")
})
public class UserAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
