package com.feple.feple_backend.comment.entity;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE comment SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL AND blinded = false")
@Table(name = "comment", indexes = {
    @Index(name = "idx_comment_post_id_created_at", columnList = "post_id, created_at ASC"),
    @Index(name = "idx_comment_user_id_created_at", columnList = "user_id, created_at DESC")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 관리자 댓글 목록(findByPostIdIgnoringBlindOrderByCreatedAtAsc)은 네이티브 쿼리라 EntityGraph로
    // 즉시 로딩할 수 없으므로, BatchSize로 지연 로딩 시 N+1을 방지한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @BatchSize(size = 20)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 답글은 depth 1단계로 평탄화되어 parent가 최상위 댓글을 가리키므로, 실제로 답글을 향한
    // 대상(댓글을 작성할 당시 "답글" 버튼을 누른 그 댓글의 작성자)은 따로 기록해야 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentioned_user_id")
    @BatchSize(size = 20)
    private User mentionedUser;

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean anonymous = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean blinded = false;

    public Comment(String content, Post post, User user, Comment parent, User mentionedUser, boolean anonymous) {
        this.content = content;
        this.post = post;
        this.user = user;
        this.parent = parent;
        this.mentionedUser = mentionedUser;
        this.anonymous = anonymous;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void blind() {
        this.blinded = true;
    }

    public void unblind() {
        this.blinded = false;
    }

    public Long getParentId() { return parent != null ? parent.getId() : null; }
    public Long getMentionedUserId() { return mentionedUser != null ? mentionedUser.getId() : null; }
    public String getMentionedNickname() { return mentionedUser != null ? mentionedUser.getNickname() : null; }
    public Long getPostId() { return post.getId(); }
    public String getPostTitle() { return post.getTitle(); }
    public Long getUserId() { return user.getId(); }
    public String getUserNickname() { return user.getNickname(); }
    public UserRole getUserRole() { return user.getRole(); }
    public String getUserProfileImageUrl() { return user.getProfileImageUrl(); }
}
