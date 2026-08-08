package com.feple.feple_backend.comment.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.entity.ResolvableReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "comment_report",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reporter_id", "comment_id"})
    },
    indexes = {
        @Index(name = "idx_comment_report_status", columnList = "status")
    }
)
public class CommentReport extends BaseTimeEntity implements ResolvableReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

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
    private String detail;

    // comment는 @ManyToOne(comment_id NOT NULL)이라 항상 존재해야 하지만, Comment의 @SQLRestriction이
    // blinded=true를 제외하면서 관리자 신고 목록 조회(EntityGraph LEFT JOIN)에서는 블라인드된
    // 댓글에 대해 comment가 null로 채워진다 — 신고 자체(CommentReport)는 그대로 남아있으므로 여기서
    // NPE 없이 안전한 값을 반환해야 관리자가 블라인드된 댓글의 신고도 목록에서 확인할 수 있다.
    public Long getCommentId() { return comment != null ? comment.getId() : null; }
    public String getCommentContent() { return comment != null ? comment.getContent() : "(블라인드된 댓글)"; }
    public Long getCommentPostId() { return comment != null ? comment.getPostId() : null; }
    public String getCommentPostTitle() { return comment != null ? comment.getPostTitle() : null; }
    public String getCommentUserNickname() { return comment != null ? comment.getUserNickname() : null; }
    public Long getCommentAuthorId() { return comment != null ? comment.getUserId() : null; }
    public String getReporterNickname() { return reporter.getNickname(); }
    public boolean isPending() { return status == ReportStatus.PENDING; }

    public void resolve(ReportStatus newStatus) {
        this.status = newStatus;
    }
}
