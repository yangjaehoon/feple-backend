package com.feple.feple_backend.post.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

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

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public PostDraft(Long userId, PostDraftContent draft) {
        this.userId = userId;
        applyContent(draft);
    }

    public void update(PostDraftContent draft) {
        applyContent(draft);
    }

    private void applyContent(PostDraftContent draft) {
        this.title = draft.title();
        this.content = draft.content();
        this.boardType = draft.boardType();
        this.anonymous = draft.anonymous();
        this.artistId = draft.artistId();
        this.festivalId = draft.festivalId();
        this.imageKeysCsv = draft.imageKeysCsv();
        // updatedAt은 @UpdateTimestamp가 insert/update flush 시점에 채운다
    }

    public List<String> getImageKeys() {
        if (imageKeysCsv == null || imageKeysCsv.isBlank()) return List.of();
        return Arrays.stream(imageKeysCsv.split(",")).toList();
    }

    public static String toImageKeysCsv(List<String> imageKeys) {
        return (imageKeys == null || imageKeys.isEmpty()) ? null : String.join(",", imageKeys);
    }
}
