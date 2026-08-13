package com.feple.feple_backend.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/** 00:00~09:00(KST) 사이 생성된 자동 FCM 푸시를 오전 9시까지 보관하는 대기열 항목 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "pending_pushes")
public class PendingPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String body;

    @Column(length = 100)
    private String titleEn;

    @Column(length = 255)
    private String bodyEn;

    @Column(length = 255)
    private String resourceId;

    @Column(length = 500)
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "pending_push_recipients", joinColumns = @JoinColumn(name = "pending_push_id"))
    @Column(name = "user_id", nullable = false)
    @Builder.Default
    private List<Long> userIds = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
