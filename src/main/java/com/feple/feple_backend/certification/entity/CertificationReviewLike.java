package com.feple.feple_backend.certification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "certification_review_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "certification_id"})
)
public class CertificationReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "certification_id", nullable = false)
    private Long certificationId;

    public static CertificationReviewLike of(Long userId, Long certificationId) {
        CertificationReviewLike like = new CertificationReviewLike();
        like.userId = userId;
        like.certificationId = certificationId;
        return like;
    }
}
