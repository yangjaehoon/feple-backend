package com.feple.feple_backend.post.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE post SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL AND blinded = false")
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
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean pinned = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean blinded = false;

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

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void togglePinned() {
        this.pinned = !this.pinned;
    }

    public void blind() {
        this.blinded = true;
    }

    public void unblind() {
        this.blinded = false;
    }

    @Builder.Default
    private int commentCount = 0;

    public int getCommentCount() { return commentCount; }

    @Builder.Default
    @OneToMany(mappedBy = "post")
    private List<Comment> comments = new ArrayList<>();

    // 목록 조회 시 게시글마다 별도 쿼리가 나가지 않도록 BatchSize로 묶어 조회한다
    // (JOIN FETCH는 페이지네이션과 함께 쓰면 결과가 뒤틀리므로 사용하지 않음).
    @Builder.Default
    @OneToMany(mappedBy = "post")
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 20)
    private List<PostImage> images = new ArrayList<>();

    public List<String> getImageKeys() {
        return images.stream().map(PostImage::getImageKey).toList();
    }

    @Builder.Default
    @OneToMany(mappedBy = "post")
    @BatchSize(size = 20)
    private List<PostTag> tags = new ArrayList<>();

    public List<String> getTagNames() {
        return tags.stream().map(PostTag::getTag).toList();
    }

    public String getBoardDisplayName() {
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

    public String getFestivalPosterKey() {
        return festival != null ? festival.getPosterKey() : null;
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

    public String getAuthorLevel() {
        return user.getLevel().name();
    }
}
