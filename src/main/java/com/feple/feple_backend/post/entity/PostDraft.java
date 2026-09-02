package com.feple.feple_backend.post.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // Post.images(PostImage)와 동일하게 첨부 이미지 키를 순서 보존 자식 테이블로 분리한다
    // (쉼표 구분 단일 컬럼은 1NF 위반). draft는 유저당 1행이라 List로 두고 @OrderColumn으로 순서를 관리한다.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_draft_image", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_key", length = 255)
    private List<String> imageKeys = new ArrayList<>();

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 정적 팩토리 — 생성자에서 값 채우기 로직(스트림 등)을 호출하면 SpotBugs CT_CONSTRUCTOR_THROW가
    // 잡는다(부분 초기화 객체 노출 위험). FestivalCertification.create()와 동일한 패턴.
    public static PostDraft create(Long userId, PostDraftContent draft) {
        PostDraft postDraft = new PostDraft();
        postDraft.userId = userId;
        postDraft.applyContent(draft);
        return postDraft;
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
        this.imageKeys.clear();
        if (draft.imageKeys() != null) {
            draft.imageKeys().stream()
                    .filter(key -> key != null && !key.isBlank())
                    .forEach(this.imageKeys::add);
        }
        // updatedAt은 @UpdateTimestamp가 insert/update flush 시점에 채운다
    }

    public List<String> getImageKeys() {
        return List.copyOf(imageKeys);
    }
}
