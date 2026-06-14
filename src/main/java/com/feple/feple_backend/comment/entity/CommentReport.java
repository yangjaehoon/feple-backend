package com.feple.feple_backend.comment.entity;

import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.Resolvable;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
public class CommentReport implements Resolvable {

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getCommentId() { return comment.getId(); }
    public String getCommentContent() { return comment.getContent(); }
    public Long getCommentPostId() { return comment.getPostId(); }
    public String getCommentPostTitle() { return comment.getPostTitle(); }
    public String getCommentUserNickname() { return comment.getUserNickname(); }
    public Long getCommentAuthorId() { return comment.getUserId(); }
    public String getReporterNickname() { return reporter.getNickname(); }
    public boolean isPending() { return status == ReportStatus.PENDING; }

    public void resolve(ReportStatus newStatus) {
        this.status = newStatus;
    }
}
