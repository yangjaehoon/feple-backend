package com.feple.feple_backend.appconfig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.appconfig.FeatureFlags;
import com.feple.feple_backend.appconfig.dto.AppConfigFormDto;
import com.feple.feple_backend.appconfig.dto.AppConfigResponseDto;
import com.feple.feple_backend.appconfig.entity.AppConfig;
import com.feple.feple_backend.appconfig.repository.AppConfigRepository;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigServiceImpl implements AppConfigService, AppConfigAdminService {

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AppConfigResponseDto getAppConfig() {
        AppConfig config = appConfigRepository.findById(AppConfig.SINGLETON_ID)
                .orElseGet(AppConfig::defaults);
        return new AppConfigResponseDto(
                config.getMinSupportedVersion(),
                config.getLatestVersion(),
                config.isMaintenance(),
                blankToNull(config.getMaintenanceMessage()),
                blankToNull(config.getNoticeMessage()),
                readFlagsLenient(config.getFeatureFlags()));
    }

    @Override
    @Transactional(readOnly = true)
    public AppConfigFormDto getConfigForm() {
        AppConfig config = appConfigRepository.findById(AppConfig.SINGLETON_ID)
                .orElseGet(AppConfig::defaults);
        return AppConfigFormDto.builder()
                .minSupportedVersion(config.getMinSupportedVersion())
                .latestVersion(config.getLatestVersion())
                .maintenance(config.isMaintenance())
                .maintenanceMessage(config.getMaintenanceMessage())
                .noticeMessage(config.getNoticeMessage())
                .featureFlags(config.getFeatureFlags())
                .build();
    }

    @Override
    @Transactional
    public void updateConfig(AppConfigFormDto form) {
        // featureFlags JSON 검증은 컨트롤러에서도 폼 에러로 처리하지만, 서비스가 다른 경로로
        // 호출될 때를 대비해 여기서도 정규화 과정에서 잘못된 입력을 거른다.
        String normalizedFlags = FeatureFlags.normalize(objectMapper, form.getFeatureFlags());
        AppConfig config = appConfigRepository.findById(AppConfig.SINGLETON_ID)
                .orElseGet(() -> appConfigRepository.save(AppConfig.defaults()));
        config.update(
                form.getMinSupportedVersion(),
                form.getLatestVersion(),
                form.isMaintenance(),
                blankToNull(form.getMaintenanceMessage()),
                blankToNull(form.getNoticeMessage()),
                normalizedFlags);
    }

    /** 조회 경로: 저장된 JSON이 깨져 있어도 앱이 멈추지 않도록 빈 맵으로 대체한다. */
    private Map<String, Boolean> readFlagsLenient(String json) {
        try {
            return FeatureFlags.parse(objectMapper, json);
        } catch (InvalidRequestException e) {
            log.warn("[AppConfig] feature_flags JSON 파싱 실패 — 빈 맵으로 대체. value={}", json);
            return Map.of();
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
