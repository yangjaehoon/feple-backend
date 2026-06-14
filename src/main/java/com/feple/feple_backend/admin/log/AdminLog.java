package com.feple.feple_backend.admin.log;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_logs", indexes = {
        @Index(name = "idx_admin_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_admin_logs_target_type", columnList = "target_type")
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

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 500)
    private String detail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
