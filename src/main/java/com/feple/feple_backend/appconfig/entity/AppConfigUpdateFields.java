package com.feple.feple_backend.appconfig.entity;

/** AppConfig.update()에 넘기는 수정 가능 필드 묶음 — 6개 개별 인수 대신 사용 */
public record AppConfigUpdateFields(
        String minSupportedVersion,
        String latestVersion,
        boolean maintenance,
        String maintenanceMessage,
        String noticeMessage,
        String featureFlags
) {}
