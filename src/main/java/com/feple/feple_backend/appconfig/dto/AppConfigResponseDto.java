package com.feple.feple_backend.appconfig.dto;

import java.util.Map;

/**
 * 클라이언트가 콜드스타트 시 받는 앱 전역 설정.
 *
 * @param minSupportedVersion 이 버전 미만이면 강제 업데이트
 * @param latestVersion       스토어 최신 버전(이 미만이면 권장 업데이트 안내)
 * @param maintenance         점검 모드 여부
 * @param maintenanceMessage  점검 안내 문구(없으면 null)
 * @param noticeMessage       홈 상단 배너 문구(없으면 null)
 * @param features            기능별 on/off 스위치
 */
public record AppConfigResponseDto(
        String minSupportedVersion,
        String latestVersion,
        boolean maintenance,
        String maintenanceMessage,
        String noticeMessage,
        Map<String, Boolean> features) {
}
