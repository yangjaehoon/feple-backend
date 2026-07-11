package com.feple.feple_backend.certification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "review_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "certification_id"})
)
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "certification_id", nullable = false)
    private Long certificationId;

    public static ReviewLike of(Long userId, Long certificationId) {
        ReviewLike like = new ReviewLike();
        like.userId = userId;
        like.certificationId = certificationId;
        return like;
    }
}
