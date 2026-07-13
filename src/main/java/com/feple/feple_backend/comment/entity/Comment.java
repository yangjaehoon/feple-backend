package com.feple.feple_backend.comment.entity;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE comment SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean anonymous = false;

    public Comment(String content, Post post, User user, boolean anonymous) {
        this.content = content;
        this.post = post;
        this.user = user;
        this.anonymous = anonymous;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Comment(String content, Post post, User user, Comment parent, boolean anonymous) {
        this.content = content;
        this.post = post;
        this.user = user;
        this.parent = parent;
        this.anonymous = anonymous;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getParentId() { return parent != null ? parent.getId() : null; }
    public Long getPostId() { return post.getId(); }
    public String getPostTitle() { return post.getTitle(); }
    public Long getUserId() { return user.getId(); }
    public String getUserNickname() { return user.getNickname(); }
    public UserRole getUserRole() { return user.getRole(); }
    public String getUserProfileImageUrl() { return user.getProfileImageUrl(); }
}
