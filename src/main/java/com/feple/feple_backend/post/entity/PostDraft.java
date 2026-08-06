package com.feple.feple_backend.post.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저당 1개만 유지하는 게시글 임시저장 — 새로 저장하면 기존 내용을 덮어쓴다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_draft")
public class PostDraft {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    private boolean anonymous;

    private Long artistId;

    private Long festivalId;

    @Column(name = "image_keys", columnDefinition = "TEXT")
    private String imageKeysCsv;

    private LocalDateTime updatedAt;

    @Builder
    public PostDraft(Long userId, String title, String content, BoardType boardType, boolean anonymous,
                      Long artistId, Long festivalId, String imageKeysCsv) {
        this.userId = userId;
        applyContent(title, content, boardType, anonymous, artistId, festivalId, imageKeysCsv);
    }

    public void update(String title, String content, BoardType boardType, boolean anonymous,
                        Long artistId, Long festivalId, String imageKeysCsv) {
        applyContent(title, content, boardType, anonymous, artistId, festivalId, imageKeysCsv);
    }

    private void applyContent(String title, String content, BoardType boardType, boolean anonymous,
                               Long artistId, Long festivalId, String imageKeysCsv) {
        this.title = title;
        this.content = content;
        this.boardType = boardType;
        this.anonymous = anonymous;
        this.artistId = artistId;
        this.festivalId = festivalId;
        this.imageKeysCsv = imageKeysCsv;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getImageKeys() {
        if (imageKeysCsv == null || imageKeysCsv.isBlank()) return List.of();
        return Arrays.stream(imageKeysCsv.split(",")).toList();
    }

    public static String toImageKeysCsv(List<String> imageKeys) {
        return (imageKeys == null || imageKeys.isEmpty()) ? null : String.join(",", imageKeys);
    }
}
