package com.feple.feple_backend.appconfig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 앱 전역 설정 싱글턴. 항상 {@code id = 1} 한 행만 존재하며, Flyway 마이그레이션(V83)이 시드한다.
 * 앱 콜드스타트 시 조회되는 핵심 경로라, 행이 없어도 서비스 레이어가 안전한 기본값으로 응답한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_config")
public class AppConfig {

    /** 싱글턴 행의 고정 PK. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** 이 버전 미만 클라이언트는 강제 업데이트 대상. 세맨틱 버전 문자열(예: "1.2.0"). */
    @Column(name = "min_supported_version", nullable = false, length = 20)
    private String minSupportedVersion;

    /** 스토어에 올라간 최신 버전. 클라이언트가 이 미만이면 권장 업데이트 안내. */
    @Column(name = "latest_version", nullable = false, length = 20)
    private String latestVersion;

    /** 점검 모드. true면 앱이 점검 안내 화면만 노출한다. */
    @Column(nullable = false)
    private boolean maintenance;

    /** 점검 안내 화면에 표시할 문구. null이면 클라이언트 기본 문구 사용. */
    @Column(name = "maintenance_message", columnDefinition = "TEXT")
    private String maintenanceMessage;

    /** 홈 상단 배너 문구. null·빈 문자열이면 배너 미노출. */
    @Column(name = "notice_message", columnDefinition = "TEXT")
    private String noticeMessage;

    /** 기능별 on/off 스위치. {@code {"chatEnabled": true}} 형태의 JSON 문자열. */
    @Column(name = "feature_flags", nullable = false, columnDefinition = "TEXT")
    private String featureFlags;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private AppConfig(Long id, String minSupportedVersion, String latestVersion, boolean maintenance,
                      String maintenanceMessage, String noticeMessage, String featureFlags) {
        this.id = id;
        this.minSupportedVersion = minSupportedVersion;
        this.latestVersion = latestVersion;
        this.maintenance = maintenance;
        this.maintenanceMessage = maintenanceMessage;
        this.noticeMessage = noticeMessage;
        this.featureFlags = featureFlags;
    }

    /** 행이 없을 때 조회 경로에서 쓰는 안전한 기본값. 아무도 잠기지 않도록 최소 버전을 0으로 둔다. */
    public static AppConfig defaults() {
        return AppConfig.builder()
                .id(SINGLETON_ID)
                .minSupportedVersion("0.0.0")
                .latestVersion("0.0.0")
                .maintenance(false)
                .maintenanceMessage(null)
                .noticeMessage(null)
                .featureFlags("{}")
                .build();
    }

    public void update(AppConfigUpdateFields fields) {
        this.minSupportedVersion = fields.minSupportedVersion();
        this.latestVersion = fields.latestVersion();
        this.maintenance = fields.maintenance();
        this.maintenanceMessage = fields.maintenanceMessage();
        this.noticeMessage = fields.noticeMessage();
        this.featureFlags = fields.featureFlags();
    }
}
