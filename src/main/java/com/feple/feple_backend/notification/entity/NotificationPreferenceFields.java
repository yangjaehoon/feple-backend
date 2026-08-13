package com.feple.feple_backend.notification.entity;

/** NotificationPreference.update()에 넘기는 수정 가능 필드 묶음 — 4개 개별 인수 대신 사용 */
public record NotificationPreferenceFields(
        boolean certEnabled,
        boolean commentEnabled,
        boolean festivalEnabled,
        boolean songRequestEnabled,
        boolean quietHoursEnabled
) {}
