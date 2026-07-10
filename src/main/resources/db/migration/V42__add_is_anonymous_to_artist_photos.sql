-- 환경마다 실제 상태가 달라 방어적으로 추가한다:
-- 프로덕션에는 이미 is_anonymous 컬럼이 존재하고(경위 불명), 신규/로컬 DB에는 없음.
SET @col_count = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'artist_photos'
      AND COLUMN_NAME  = 'is_anonymous'
);

SET @add_is_anonymous = IF(
    @col_count = 0,
    'ALTER TABLE `artist_photos` ADD COLUMN `is_anonymous` TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0'
);
PREPARE s FROM @add_is_anonymous; EXECUTE s; DEALLOCATE PREPARE s;
