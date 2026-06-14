package com.feple.feple_backend.notification.dto;

import com.feple.feple_backend.notification.entity.NotificationPreference;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPreferenceDto {
    private boolean certEnabled;
    private boolean commentEnabled;
    private boolean festivalEnabled;
    private boolean songRequestEnabled;

    public static NotificationPreferenceDto from(NotificationPreference pref) {
        return NotificationPreferenceDto.builder()
                .certEnabled(pref.isCertEnabled())
                .commentEnabled(pref.isCommentEnabled())
                .festivalEnabled(pref.isFestivalEnabled())
                .songRequestEnabled(pref.isSongRequestEnabled())
                .build();
    }
}
