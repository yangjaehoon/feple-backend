package com.feple.feple_backend.comment.entity;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "comment", indexes = {
    @Index(name = "idx_comment_post_id_created_at", columnList = "post_id, created_at ASC")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private int likeCount = 0;

    public Comment(String content, Post post, User user) {
        this.content = content;
        this.post = post;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Comment(String content, Post post, User user, Long parentId) {
        this.content = content;
        this.post = post;
        this.user = user;
        this.parentId = parentId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementLikeCount() { this.likeCount++; }
    public void decrementLikeCount() { if (this.likeCount > 0) this.likeCount--; }
}
