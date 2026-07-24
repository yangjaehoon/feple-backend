package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.entity.NotificationType;

public record PushMessage(String title, String body, String resourceId, NotificationType type) {
}
