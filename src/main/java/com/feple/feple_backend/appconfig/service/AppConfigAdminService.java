package com.feple.feple_backend.appconfig.service;

import com.feple.feple_backend.appconfig.dto.AppConfigFormDto;

public interface AppConfigAdminService {

    /** 관리자 편집 폼에 채울 현재 설정. */
    AppConfigFormDto getConfigForm();

    /** 관리자 편집 폼 저장. featureFlags JSON이 유효하지 않으면 InvalidRequestException. */
    void updateConfig(AppConfigFormDto form);
}
