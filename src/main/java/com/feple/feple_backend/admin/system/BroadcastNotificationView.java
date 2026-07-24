package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.notification.entity.BroadcastNotification;

import java.time.LocalDateTime;

public record BroadcastNotificationView(
        String title,
        String body,
        LocalDateTime createdAt
) {
    public static BroadcastNotificationView from(BroadcastNotification b) {
        return new BroadcastNotificationView(b.getTitle(), b.getBody(), b.getCreatedAt());
    }
}
