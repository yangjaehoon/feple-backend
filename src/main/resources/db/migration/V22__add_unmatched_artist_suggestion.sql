CREATE TABLE unmatched_artist_suggestion (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    mention_count INT         NOT NULL DEFAULT 1,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_unmatched_name (name)
);
