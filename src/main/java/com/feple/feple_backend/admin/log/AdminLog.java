package com.feple.feple_backend.admin.log;

import static jakarta.persistence.EnumType.STRING;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_logs", indexes = {
        @Index(name = "idx_admin_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_admin_logs_target_type", columnList = "target_type"),
        @Index(name = "idx_admin_logs_admin_username", columnList = "admin_username")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AdminLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", length = 100)
    private String adminUsername;

    @Enumerated(STRING)
    @Column(nullable = false, length = 50)
    private AdminAction action;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 2000)
    private String detail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
