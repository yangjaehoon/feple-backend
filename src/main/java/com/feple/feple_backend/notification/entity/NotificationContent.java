package com.feple.feple_backend.notification.entity;

public record NotificationContent(NotificationType type, String title, String body, String titleEn, String bodyEn) {
}
