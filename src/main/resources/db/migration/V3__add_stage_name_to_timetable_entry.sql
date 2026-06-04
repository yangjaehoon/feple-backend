-- timetable_entry.stage_name 컬럼 정규화
-- 로컬: 컬럼 없음 → ADD COLUMN NOT NULL DEFAULT 'main'
-- 프로덕션: 컬럼 이미 존재(NOT NULL, no default) → NULL 행 보정 후 DEFAULT 'main' 추가
DROP PROCEDURE IF EXISTS `migrate_timetable_stage_name`;

CREATE PROCEDURE `migrate_timetable_stage_name`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'timetable_entry'
          AND COLUMN_NAME  = 'stage_name'
    ) THEN
        ALTER TABLE `timetable_entry`
            ADD COLUMN `stage_name` VARCHAR(255) NOT NULL DEFAULT 'main';
    ELSE
        UPDATE `timetable_entry` SET `stage_name` = 'main' WHERE `stage_name` IS NULL;
        ALTER TABLE `timetable_entry`
            MODIFY COLUMN `stage_name` VARCHAR(255) NOT NULL DEFAULT 'main';
    END IF;
END;

CALL `migrate_timetable_stage_name`();
DROP PROCEDURE IF EXISTS `migrate_timetable_stage_name`;
