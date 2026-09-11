package com.feple.feple_backend.appconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.appconfig.dto.AppConfigFormDto;
import com.feple.feple_backend.appconfig.dto.AppConfigResponseDto;
import com.feple.feple_backend.appconfig.entity.AppConfig;
import com.feple.feple_backend.appconfig.repository.AppConfigRepository;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppConfigServiceImplTest {

    @Mock
    AppConfigRepository appConfigRepository;

    AppConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppConfigServiceImpl(appConfigRepository, new ObjectMapper());
    }

    private AppConfig config(String minVersion, String latestVersion, boolean maintenance,
                             String maintenanceMessage, String noticeMessage, String featureFlags) {
        return AppConfig.builder()
                .id(AppConfig.SINGLETON_ID)
                .minSupportedVersion(minVersion)
                .latestVersion(latestVersion)
                .maintenance(maintenance)
                .maintenanceMessage(maintenanceMessage)
                .noticeMessage(noticeMessage)
                .featureFlags(featureFlags)
                .build();
    }

    private AppConfigFormDto form(String minVersion, String latestVersion, String featureFlags) {
        return AppConfigFormDto.builder()
                .minSupportedVersion(minVersion)
                .latestVersion(latestVersion)
                .maintenance(false)
                .maintenanceMessage(null)
                .noticeMessage(null)
                .featureFlags(featureFlags)
                .build();
    }

    @Test
    void 설정_행이_있으면_저장된_값으로_응답한다() {
        given(appConfigRepository.findById(AppConfig.SINGLETON_ID))
                .willReturn(Optional.of(config("1.2.0", "1.3.0", true,
                        "점검 중", "배너", "{\"chatEnabled\":true,\"uploadEnabled\":false}")));

        AppConfigResponseDto result = service.getAppConfig();

        assertThat(result.minSupportedVersion()).isEqualTo("1.2.0");
        assertThat(result.latestVersion()).isEqualTo("1.3.0");
        assertThat(result.maintenance()).isTrue();
        assertThat(result.maintenanceMessage()).isEqualTo("점검 중");
        assertThat(result.noticeMessage()).isEqualTo("배너");
        assertThat(result.features()).containsEntry("chatEnabled", true).containsEntry("uploadEnabled", false);
    }

    @Test
    void 설정_행이_없으면_아무도_잠기지_않는_기본값을_반환한다() {
        given(appConfigRepository.findById(AppConfig.SINGLETON_ID)).willReturn(Optional.empty());

        AppConfigResponseDto result = service.getAppConfig();

        assertThat(result.minSupportedVersion()).isEqualTo("0.0.0");
        assertThat(result.maintenance()).isFalse();
        assertThat(result.features()).isEmpty();
    }

    @Test
    void 저장된_featureFlags_JSON이_깨져_있으면_빈_맵으로_대체한다() {
        given(appConfigRepository.findById(AppConfig.SINGLETON_ID))
                .willReturn(Optional.of(config("1.0.0", "1.0.0", false, null, null, "not-json")));

        AppConfigResponseDto result = service.getAppConfig();

        assertThat(result.features()).isEmpty();
    }

    @Test
    void 빈_문구는_null로_응답한다() {
        given(appConfigRepository.findById(AppConfig.SINGLETON_ID))
                .willReturn(Optional.of(config("1.0.0", "1.0.0", false, "   ", "", "{}")));

        AppConfigResponseDto result = service.getAppConfig();

        assertThat(result.maintenanceMessage()).isNull();
        assertThat(result.noticeMessage()).isNull();
    }

    @Test
    void updateConfig는_엔티티에_정규화된_값을_반영한다() {
        AppConfig existing = config("1.0.0", "1.0.0", false, null, null, "{}");
        given(appConfigRepository.findById(AppConfig.SINGLETON_ID)).willReturn(Optional.of(existing));

        service.updateConfig(form("1.2.0", "1.3.0", "{\"chatEnabled\": true}"));

        assertThat(existing.getMinSupportedVersion()).isEqualTo("1.2.0");
        assertThat(existing.getLatestVersion()).isEqualTo("1.3.0");
        assertThat(existing.getFeatureFlags()).isEqualTo("{\"chatEnabled\":true}");
    }

    @Test
    void updateConfig는_빈_featureFlags를_빈_JSON_객체로_저장한다() {
        AppConfig existing = config("1.0.0", "1.0.0", false, null, null, "{}");
        given(appConfigRepository.findById(AppConfig.SINGLETON_ID)).willReturn(Optional.of(existing));

        service.updateConfig(form("1.0.0", "1.0.0", "  "));

        assertThat(existing.getFeatureFlags()).isEqualTo("{}");
    }

    @Test
    void updateConfig는_잘못된_featureFlags_JSON이면_저장하지_않고_예외를_던진다() {
        assertThatThrownBy(() -> service.updateConfig(form("1.0.0", "1.0.0", "oops")))
                .isInstanceOf(InvalidRequestException.class);

        then(appConfigRepository).should(never()).findById(any());
    }
}
