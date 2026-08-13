-- 새벽(00:00~09:00 KST) 사이 자동 생성된 FCM 푸시(댓글/좋아요 제외)를 즉시 보내지 않고
-- 모아뒀다가 매일 09:00 KST에 한 번에 발송하기 위한 대기열. 서버 재배포로 유실되지 않도록
-- 메모리 스케줄링이 아닌 DB에 적재한다.
CREATE TABLE IF NOT EXISTS `pending_pushes` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `type`        VARCHAR(30)  NOT NULL,
    `title`       VARCHAR(100) NOT NULL,
    `body`        VARCHAR(255) NOT NULL,
    `title_en`    VARCHAR(100) NULL,
    `body_en`     VARCHAR(255) NULL,
    `resource_id` VARCHAR(255) NULL,
    `image_url`   VARCHAR(500) NULL,
    `created_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `pending_push_recipients` (
    `pending_push_id` BIGINT NOT NULL,
    `user_id`         BIGINT NOT NULL,
    KEY `idx_pending_push_recipients_push_id` (`pending_push_id`),
    CONSTRAINT `fk_pending_push_recipients_push` FOREIGN KEY (`pending_push_id`) REFERENCES `pending_pushes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
