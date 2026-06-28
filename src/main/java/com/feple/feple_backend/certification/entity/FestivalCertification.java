package com.feple.feple_backend.certification.entity;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "festival_id"})
    },
    indexes = {
        @Index(name = "idx_festival_cert_status", columnList = "status")
    }
)
public class FestivalCertification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false)
    private String photoKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificationStatus status;

    @Column(length = 500)
    private String rejectionMessage;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    @Column
    private Integer rating;

    @Column(length = 100)
    private String userReview;

    private LocalDateTime ratedAt;

    public Long getFestivalId() { return festival.getId(); }
    public String getFestivalTitle() { return festival.getTitle(); }
    public String getFestivalTitleEn() { return festival.getTitleEn(); }
    public String getFestivalPosterKey() { return festival.getPosterKey(); }
    public Long getUserId() { return user.getId(); }
    public String getUserNickname() { return user.getNickname(); }
    public String getUserEmail() { return user.getEmail(); }
    public boolean isPending() { return status == CertificationStatus.PENDING; }
    public boolean isApproved() { return status == CertificationStatus.APPROVED; }

    public static FestivalCertification create(User user, Festival festival, String photoKey) {
        FestivalCertification cert = new FestivalCertification();
        cert.user = user;
        cert.festival = festival;
        cert.photoKey = photoKey;
        cert.status = CertificationStatus.PENDING;
        return cert;
    }

    public void approve(String reviewerName) {
        this.status = CertificationStatus.APPROVED;
        this.rejectionMessage = null;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewerName;
    }

    public void reject(String message, String reviewerName) {
        this.status = CertificationStatus.REJECTED;
        this.rejectionMessage = message;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewerName;
    }

    public void rate(int rating, String review) {
        this.rating = rating;
        this.userReview = review;
        this.ratedAt = LocalDateTime.now();
    }
}
