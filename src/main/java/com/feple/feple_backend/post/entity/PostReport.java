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

    // post는 @ManyToOne(post_id NOT NULL)이지만, 대상 글이 하드 삭제되면 관리자 신고 목록
    // 조회(EntityGraph LEFT JOIN)에서 post가 null로 채워질 수 있다 — 신고 자체(PostReport)는
    // 남으므로 여기서 NPE 없이 안전한 값을 반환한다.
    public Long getPostId() { return post != null ? post.getId() : null; }
    public String getPostTitle() { return post != null ? post.getTitle() : "(삭제된 게시글)"; }
    public String getAuthorNickname() { return post != null ? post.getAuthorNickname() : null; }
    public Long getPostAuthorId() { return post != null ? post.getUserId() : null; }
    public String getReporterNickname() { return reporter.getNickname(); }
    public boolean isPending() { return status == ReportStatus.PENDING; }

    public void resolve(ReportStatus newStatus) {
        this.status = newStatus;
    }
}
