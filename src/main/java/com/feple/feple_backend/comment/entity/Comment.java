package com.feple.feple_backend.comment.entity;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// update()/blind()/unblind()의 더티체킹 flush가 전체 컬럼을 UPDATE하면서, 동시에 다른 트랜잭션이
// incrementLikeCount/decrementLikeCount로 원자적으로 갱신한 likeCount를 로드 시점 값으로 덮어쓰는 것을 방지한다.
@DynamicUpdate
// 공개 조회 가시성(삭제·블라인드 제외)은 @SQLRestriction 상시 필터 대신 CommentRepository의
// 공개 쿼리에 deleted_at IS NULL AND blinded = false를 명시한다 (Festival/Artist와 동일 방식).
@SQLDelete(sql = "UPDATE comment SET deleted_at = NOW() WHERE id = ?")
@Table(name = "comment", indexes = {
    @Index(name = "idx_comment_post_id_created_at", columnList = "post_id, created_at ASC"),
    @Index(name = "idx_comment_user_id_created_at", columnList = "user_id, created_at DESC")
})
public class Comment {

    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 답글은 depth 1단계로 평탄화되어 parent가 최상위 댓글을 가리키므로, 실제로 답글을 향한
    // 대상(댓글을 작성할 당시 "답글" 버튼을 누른 그 댓글의 작성자)은 따로 기록해야 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentioned_user_id")
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
        // createdAt/updatedAt은 @CreationTimestamp/@UpdateTimestamp가 flush 시점에 채운다
    }

    public void update(String content) {
        this.content = content;
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
