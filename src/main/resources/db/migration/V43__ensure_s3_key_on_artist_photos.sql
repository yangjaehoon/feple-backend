-- 환경마다 실제 상태가 달라 방어적으로 추가한다:
-- V1 baseline은 CREATE TABLE IF NOT EXISTS라 이미 테이블이 존재하던 환경에서는
-- s3_key 컬럼 추가가 조용히 스킵되어 컬럼이 누락될 수 있음.
SET @col_count = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'artist_photos'
      AND COLUMN_NAME  = 's3_key'
);

SET @add_s3_key = IF(
    @col_count = 0,
    'ALTER TABLE `artist_photos` ADD COLUMN `s3_key` VARCHAR(500) NOT NULL DEFAULT '''' AFTER `uploader_user_id`',
    'DO 0'
);
PREPARE s FROM @add_s3_key; EXECUTE s; DEALLOCATE PREPARE s;
