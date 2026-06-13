package com.feple.feple_backend.post.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE post SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "post", indexes = {
    @Index(name = "idx_post_board_type_created_at", columnList = "board_type, created_at DESC"),
    @Index(name = "idx_post_like_count_created_at", columnList = "like_count DESC, created_at DESC"),
    // 커서 페이지네이션: WHERE board_type = ? [AND id < ?] ORDER BY id DESC
    @Index(name = "idx_post_board_type_id", columnList = "board_type, id DESC"),
    // 아티스트/페스티벌 게시판, 마이페이지
    @Index(name = "idx_post_artist_id_created_at", columnList = "artist_id, created_at DESC"),
    @Index(name = "idx_post_festival_id_created_at", columnList = "festival_id, created_at DESC"),
    @Index(name = "idx_post_user_id_created_at", columnList = "user_id, created_at DESC")
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

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean anonymous = false;

    @Builder.Default
    private int viewCount = 0;

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

    public void update(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0)
            this.likeCount--;
    }

    public void incrementScrapCount() { this.scrapCount++; }

    public void decrementScrapCount() {
        if (this.scrapCount > 0) this.scrapCount--;
    }

    @Builder.Default
    private int commentCount = 0;

    public int getCommentCount() { return commentCount; }

    public void incrementCommentCount() { this.commentCount++; }

    public void decrementCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }

    @Builder.Default
    @OneToMany(mappedBy = "post")
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
