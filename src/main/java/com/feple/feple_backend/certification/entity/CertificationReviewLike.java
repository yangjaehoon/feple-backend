package com.feple.feple_backend.certification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
