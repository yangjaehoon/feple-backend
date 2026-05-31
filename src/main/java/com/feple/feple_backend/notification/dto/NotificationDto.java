package com.feple.feple_backend.notification.dto;

import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    public static NotificationDto forBroadcast(BroadcastNotification b) {
        return new NotificationDto(
                b.getId(),
                NotificationType.ADMIN_BROADCAST,
                b.getTitle(),
                b.getBody(),
                null,
                true,
                b.getCreatedAt()
        );
    }
}
