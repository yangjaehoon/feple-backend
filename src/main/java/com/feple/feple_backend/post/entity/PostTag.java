package com.feple.feple_backend.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_tag", indexes = {
    @Index(name = "idx_post_tag_tag", columnList = "tag")
})
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 30)
    private String tag;

    @Builder
    public PostTag(Post post, String tag) {
        this.post = post;
        this.tag = tag;
    }
}
