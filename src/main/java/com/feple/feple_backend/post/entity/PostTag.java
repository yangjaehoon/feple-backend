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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_tag",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_post_tag_post_id_tag", columnNames = {"post_id", "tag"})
    },
    indexes = {
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
