package com.feple.feple_backend.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BroadcastNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static BroadcastNotification of(String title, String body) {
        BroadcastNotification b = new BroadcastNotification();
        b.title = title;
        b.body = body;
        return b;
    }
}
