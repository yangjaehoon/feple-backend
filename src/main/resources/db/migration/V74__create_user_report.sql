CREATE TABLE IF NOT EXISTS `user_report` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `target_id`   BIGINT          NOT NULL,
    `reporter_id` BIGINT          NOT NULL,
    `reason`      VARCHAR(255)    NOT NULL,
    `status`      VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
    `detail`      VARCHAR(255)    NULL,
    `created_at`  DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ur_reporter_target` (`reporter_id`, `target_id`),
    KEY `idx_user_report_status` (`status`),
    CONSTRAINT `fk_ur_target`   FOREIGN KEY (`target_id`)   REFERENCES `users` (`id`),
    CONSTRAINT `fk_ur_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
