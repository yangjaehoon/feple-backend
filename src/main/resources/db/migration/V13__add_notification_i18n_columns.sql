-- notifications 테이블에 다국어 컬럼 추가
-- Notification 엔티티의 titleEn, bodyEn 필드가 프로덕션 스키마에 누락되어 있어 추가

ALTER TABLE `notifications`
    ADD COLUMN IF NOT EXISTS `title_en` VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS `body_en` TEXT NULL;
