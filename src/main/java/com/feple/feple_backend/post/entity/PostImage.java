package com.feple.feple_backend.post.entity;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_image", indexes = {
    @Index(name = "idx_post_image_post_id", columnList = "post_id, sort_order")
})
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public PostImage(Post post, String imageKey, int sortOrder) {
        this.post = post;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}
