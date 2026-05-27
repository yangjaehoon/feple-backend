package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.dto.NotificationPreferenceDto;
import com.feple.feple_backend.notification.dto.UpdateNotificationPreferenceDto;
import com.feple.feple_backend.notification.entity.NotificationPreference;

public interface NotificationPreferenceService {
    NotificationPreferenceDto getPreferences(Long userId);
    NotificationPreferenceDto updatePreferences(Long userId, UpdateNotificationPreferenceDto dto);
    NotificationPreference getOrCreate(Long userId);
}
