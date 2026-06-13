-- notifications 테이블에 다국어 컬럼 추가 (MySQL 8.x 호환 idempotent 방식)

SET @title_en_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'notifications'
      AND COLUMN_NAME  = 'title_en'
);

SET @add_title_en = IF(
    @title_en_exists = 0,
    'ALTER TABLE `notifications` ADD COLUMN `title_en` VARCHAR(255) NULL',
    'DO 0'
);
PREPARE s FROM @add_title_en; EXECUTE s; DEALLOCATE PREPARE s;

SET @body_en_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'notifications'
      AND COLUMN_NAME  = 'body_en'
);

SET @add_body_en = IF(
    @body_en_exists = 0,
    'ALTER TABLE `notifications` ADD COLUMN `body_en` TEXT NULL',
    'DO 0'
);
PREPARE s FROM @add_body_en; EXECUTE s; DEALLOCATE PREPARE s;
