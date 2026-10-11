-- 리프레시 토큰 교체 유예 시간.
-- 교체(rotate) 시 옛 토큰을 바로 지우지 않고 rotated_at만 기록해, 교체 응답을 못 받은
-- 클라이언트가 유예 시간 안에 옛 토큰으로 다시 갱신할 수 있게 한다(RefreshTokenService).
--
-- V1 baseline이 만드는 CI 테스트 DB와 운영 DB의 스키마가 다를 수 있어(baseline drift)
-- information_schema로 조건부 추가한다.

SET @col_exists = (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'refresh_tokens'
      AND column_name = 'rotated_at');
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE `refresh_tokens` ADD COLUMN `rotated_at` DATETIME(6) NULL AFTER `expires_at`',
    'DO 0');
PREPARE stmt FROM @add_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 유예가 지난 교체 토큰 정리(deleteRotatedBefore)용 — expires_at 인덱스(V8)와 같은 이유.
SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'refresh_tokens'
      AND index_name = 'idx_refresh_tokens_rotated_at');
SET @idx_sql = IF(@idx_exists = 0,
    'ALTER TABLE `refresh_tokens` ADD INDEX `idx_refresh_tokens_rotated_at` (`rotated_at`)',
    'DO 0');
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
