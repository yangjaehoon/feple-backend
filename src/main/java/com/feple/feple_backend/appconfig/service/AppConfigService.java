package com.feple.feple_backend.appconfig.service;

import com.feple.feple_backend.appconfig.dto.AppConfigResponseDto;

public interface AppConfigService {

    /** 클라이언트 콜드스타트용 앱 전역 설정 조회. 설정 행이 없어도 안전한 기본값을 반환한다. */
    AppConfigResponseDto getAppConfig();
}
