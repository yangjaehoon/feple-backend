package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.dto.NotificationPreferenceDto;
import com.feple.feple_backend.notification.dto.UpdateNotificationPreferenceDto;
import com.feple.feple_backend.notification.entity.NotificationPreference;

import java.util.List;
import java.util.Map;

public interface NotificationPreferenceService {
    NotificationPreferenceDto getPreferences(Long userId);
    void updatePreferences(Long userId, UpdateNotificationPreferenceDto dto);
    NotificationPreference getOrCreate(Long userId);
    Map<Long, NotificationPreference> getOrCreateBatch(List<Long> userIds);
}
