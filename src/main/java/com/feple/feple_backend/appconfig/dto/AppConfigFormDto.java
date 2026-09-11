package com.feple.feple_backend.appconfig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 앱 설정 편집 폼 바인딩 객체. {@code featureFlags}는 관리자가 직접 편집하는 원본 JSON 문자열이며,
 * 유효성(파싱 가능 여부)은 서비스 레이어에서 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfigFormDto {

    /** 세맨틱 버전 형식: 숫자.숫자.숫자 */
    public static final String VERSION_PATTERN = "\\d+\\.\\d+\\.\\d+";
    private static final String VERSION_MESSAGE = "버전은 1.2.0 형식(숫자.숫자.숫자)이어야 합니다.";

    @NotBlank(message = "최소 지원 버전을 입력해 주세요.")
    @Pattern(regexp = VERSION_PATTERN, message = VERSION_MESSAGE)
    private String minSupportedVersion;

    @NotBlank(message = "최신 버전을 입력해 주세요.")
    @Pattern(regexp = VERSION_PATTERN, message = VERSION_MESSAGE)
    private String latestVersion;

    private boolean maintenance;

    @Size(max = 2000, message = "점검 안내 문구는 2000자 이하여야 합니다.")
    private String maintenanceMessage;

    @Size(max = 2000, message = "공지 배너 문구는 2000자 이하여야 합니다.")
    private String noticeMessage;

    @Size(max = 4000, message = "기능 스위치 JSON은 4000자 이하여야 합니다.")
    private String featureFlags;
}
