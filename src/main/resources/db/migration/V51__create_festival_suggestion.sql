-- festival_suggestion: 사용자가 미등록 페스티벌을 신청하는 테이블 (artist_suggestion과 동일 패턴)
CREATE TABLE IF NOT EXISTS `festival_suggestion` (
    `id`                   BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`              BIGINT          NOT NULL,
    `festival_name`        VARCHAR(255)    NOT NULL,
    `note`                 VARCHAR(255)    NULL,
    `process_note`         VARCHAR(500)    NULL,
    `status`               VARCHAR(255)    NOT NULL,
    `created_at`           DATETIME(6)     NOT NULL,
    `processed_at`         DATETIME(6)     NULL,
    `approved_festival_id` BIGINT          NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_fs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    INDEX `idx_festival_suggestion_user_id` (`user_id`),
    INDEX `idx_festival_suggestion_status_created_at` (`status`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
