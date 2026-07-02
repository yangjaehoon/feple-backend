package com.feple.feple_backend.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    NEW_FESTIVAL(PreferenceCategory.FESTIVAL),
    CERT_APPROVED(PreferenceCategory.CERTIFICATION),
    CERT_REJECTED(PreferenceCategory.CERTIFICATION),
    NEW_COMMENT(PreferenceCategory.COMMENT),
    NEW_REPLY(PreferenceCategory.COMMENT),
    POST_LIKED(PreferenceCategory.COMMENT),
    POST_DELETED_BY_ADMIN(PreferenceCategory.ALWAYS_ENABLED),
    FESTIVAL_REMINDER(PreferenceCategory.FESTIVAL),
    SONG_REQUEST_APPROVED(PreferenceCategory.SONG_REQUEST),
    SONG_REQUEST_REJECTED(PreferenceCategory.SONG_REQUEST),
    ARTIST_SUGGESTION_PROCESSED(PreferenceCategory.SONG_REQUEST),
    ADMIN_BROADCAST(PreferenceCategory.ALWAYS_ENABLED);

    private final PreferenceCategory category;
}
