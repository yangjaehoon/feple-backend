package com.feple.feple_backend.notification.dto;

import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String body,
        String titleEn,
        String bodyEn,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt,
        String imageUrl
) {
    public static NotificationDto from(Notification n, String imageUrl) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getTitleEn(),
                n.getBodyEn(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt(),
                imageUrl
        );
    }
}
