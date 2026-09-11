-- 앱 전역 설정 싱글턴 테이블. 클라이언트가 콜드스타트 시 GET /app/config 로 조회한다.
-- 강제 업데이트(min_supported_version), 권장 업데이트(latest_version), 점검 모드,
-- 홈 배너 공지, 기능별 on/off 스위치(feature_flags JSON)를 관리자 페이지에서 재배포 없이 조정한다.
-- 항상 id = 1 한 행만 존재한다.
CREATE TABLE IF NOT EXISTS `app_config` (
    `id`                    BIGINT       NOT NULL,
    `min_supported_version` VARCHAR(20)  NOT NULL DEFAULT '0.0.0',
    `latest_version`        VARCHAR(20)  NOT NULL DEFAULT '0.0.0',
    `maintenance`           TINYINT(1)   NOT NULL DEFAULT 0,
    `maintenance_message`   TEXT         NULL,
    `notice_message`        TEXT         NULL,
    `feature_flags`         TEXT         NOT NULL,
    `updated_at`            DATETIME(6)  NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 시드 행. 현재 배포된 최소 버전은 1.0.0이라 아무도 강제 업데이트에 걸리지 않는다.
-- ON DUPLICATE KEY로 idempotent — 운영 DB에 이미 행이 있어도(베이스라인 드리프트) 덮어쓰지 않는다.
INSERT INTO `app_config`
    (`id`, `min_supported_version`, `latest_version`, `maintenance`, `feature_flags`, `updated_at`)
VALUES (1, '1.0.0', '1.0.1', 0, '{}', NOW(6))
ON DUPLICATE KEY UPDATE `id` = `id`;
