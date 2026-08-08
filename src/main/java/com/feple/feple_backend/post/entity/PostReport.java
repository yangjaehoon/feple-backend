package com.feple.feple_backend.post.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.entity.ResolvableReport;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "post_report",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reporter_id", "post_id"})
    },
    indexes = {
        @Index(name = "idx_post_report_status", columnList = "status")
    }
)
public class PostReport extends BaseTimeEntity implements ResolvableReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

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

    // post는 @ManyToOne(post_id NOT NULL)이라 항상 존재해야 하지만, Post의 @SQLRestriction이
    // blinded=true를 제외하면서 관리자 신고 목록 조회(EntityGraph LEFT JOIN)에서는 블라인드된
    // 게시글에 대해 post가 null로 채워진다 — 신고 자체(PostReport)는 그대로 남아있으므로 여기서
    // NPE 없이 안전한 값을 반환해야 관리자가 블라인드된 글의 신고도 목록에서 확인할 수 있다.
    public Long getPostId() { return post != null ? post.getId() : null; }
    public String getPostTitle() { return post != null ? post.getTitle() : "(블라인드된 게시글)"; }
    public String getAuthorNickname() { return post != null ? post.getAuthorNickname() : null; }
    public Long getPostAuthorId() { return post != null ? post.getUserId() : null; }
    public String getReporterNickname() { return reporter.getNickname(); }
    public boolean isPending() { return status == ReportStatus.PENDING; }

    public void resolve(ReportStatus newStatus) {
        this.status = newStatus;
    }
}
