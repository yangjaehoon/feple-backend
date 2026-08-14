-- 페스티벌 인증(certification)과 별개로, 사용자가 사진 여러 장 + 자유 텍스트로 남기는 개인 일기.
-- 항목마다 PRIVATE/PUBLIC 공개 범위를 직접 선택할 수 있다.
CREATE TABLE IF NOT EXISTS `festival_diary` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `festival_id`  BIGINT       NOT NULL,
    `content`      TEXT         NOT NULL,
    `visibility`   VARCHAR(20)  NOT NULL,
    `created_at`   DATETIME(6)  NOT NULL,
    `updated_at`   DATETIME(6)  NULL,
    PRIMARY KEY (`id`),
    KEY `idx_festival_diary_festival_visibility` (`festival_id`, `visibility`),
    KEY `idx_festival_diary_user` (`user_id`),
    CONSTRAINT `fk_festival_diary_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_festival_diary_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `festival_diary_photo` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `diary_id`    BIGINT       NOT NULL,
    `photo_key`   VARCHAR(500) NOT NULL,
    `sort_order`  INT          NOT NULL DEFAULT 0,
    `created_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_festival_diary_photo_diary` (`diary_id`),
    CONSTRAINT `fk_festival_diary_photo_diary` FOREIGN KEY (`diary_id`) REFERENCES `festival_diary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
