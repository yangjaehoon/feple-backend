-- 게시글 임시저장 — 유저당 1개(작성 중이던 글을 덮어쓰기 저장)
CREATE TABLE IF NOT EXISTS `post_draft` (
    `user_id`     BIGINT       NOT NULL,
    `board_type`  VARCHAR(255) NULL,
    `title`       VARCHAR(255) NULL,
    `content`     TEXT         NULL,
    `anonymous`   TINYINT(1)   NOT NULL DEFAULT 0,
    `artist_id`   BIGINT       NULL,
    `festival_id` BIGINT       NULL,
    `image_keys`  TEXT         NULL,
    `updated_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_post_draft_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
