package com.feple.feple_backend.appconfig.controller;

import com.feple.feple_backend.appconfig.dto.AppConfigResponseDto;
import com.feple.feple_backend.appconfig.service.AppConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "앱 설정", description = "클라이언트 콜드스타트 시 조회하는 앱 전역 설정(강제 업데이트·점검·공지·기능 스위치)")
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/config")
    public AppConfigResponseDto getAppConfig() {
        return appConfigService.getAppConfig();
    }
}
