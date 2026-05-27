package com.feple.feple_backend.post.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "post", indexes = {
    @Index(name = "idx_post_board_type_created_at", columnList = "board_type, created_at DESC"),
    @Index(name = "idx_post_like_count_created_at", columnList = "like_count DESC, created_at DESC")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    private int likeCount;

    private int scrapCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean anonymous = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = true)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = true)
    private Festival festival;

    private Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0)
            this.likeCount--;
    }

    @Getter(AccessLevel.NONE)
    @Formula("(SELECT COUNT(*) FROM comment c WHERE c.post_id = id)")
    private int formulaCommentCount;

    public int getCommentCount() {
        return formulaCommentCount;
    }

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public String getDisplayBoardName() {
        if (artist != null) return artist.getName() + " 게시판";
        if (festival != null) return festival.getTitle() + " 게시판";
        if (boardType == BoardType.FREE) return "자유 게시판";
        if (boardType == BoardType.MATE) return "동행 게시판";
        return "게시판";
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getArtistId() {
        return artist != null ? artist.getId() : null;
    }

    public Long getFestivalId() {
        return festival != null ? festival.getId() : null;
    }

    public String getAuthorNickname() {
        return user.getNickname();
    }

    public String getAuthorProfileImageUrl() {
        return user.getProfileImageUrl();
    }

    public UserRole getAuthorRole() {
        return user.getRole();
    }
}
