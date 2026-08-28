package com.feple.feple_backend.userreport.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.entity.ResolvableReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "user_report",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reporter_id", "target_id"})
    },
    indexes = {
        @Index(name = "idx_user_report_status", columnList = "status")
    }
)
public class UserReport extends BaseTimeEntity implements ResolvableReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column
    private String detail; // 기타 사유 상세

    public Long getTargetId() { return target.getId(); }
    public String getTargetNickname() { return target.getNickname(); }
    public String getReporterNickname() { return reporter.getNickname(); }
    public boolean isPending() { return status == ReportStatus.PENDING; }

    public void resolve(ReportStatus newStatus) {
        this.status = newStatus;
    }
}
