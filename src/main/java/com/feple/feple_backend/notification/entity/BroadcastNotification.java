package com.feple.feple_backend.notification.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "broadcast_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BroadcastNotification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    public static BroadcastNotification of(String title, String body) {
        BroadcastNotification notification = new BroadcastNotification();
        notification.title = title;
        notification.body = body;
        return notification;
    }
}
