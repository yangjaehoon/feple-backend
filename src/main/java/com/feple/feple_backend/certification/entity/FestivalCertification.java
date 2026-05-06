package com.feple.feple_backend.certification.entity;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "festival_id"})
})
public class FestivalCertification {

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

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
}
