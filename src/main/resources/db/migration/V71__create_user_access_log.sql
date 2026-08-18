-- 사용자별 "하루 첫 접속" 1건만 기록하는 로그. 로그인 이벤트가 아니라 인증된 요청(JwtAuthenticationFilter)
-- 기준으로 기록하므로, 세션이 유지된 채 재로그인 없이 접속한 사용자도 매일 정확히 1행으로 집계된다.
CREATE TABLE IF NOT EXISTS `user_access_log` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `access_date` DATE        NOT NULL,
    `created_at`  DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_access_log_user_date` (`user_id`, `access_date`),
    KEY `idx_user_access_log_access_date` (`access_date`),
    CONSTRAINT `fk_user_access_log_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
