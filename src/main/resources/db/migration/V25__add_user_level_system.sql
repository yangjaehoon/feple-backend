ALTER TABLE users ADD COLUMN point INT NOT NULL DEFAULT 0;

CREATE TABLE user_point_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    delta      INT          NOT NULL,
    reason     VARCHAR(30)  NOT NULL,
    ref_id     BIGINT,
    created_at DATETIME     NOT NULL,
    INDEX idx_user_point_log_user_id (user_id),
    CONSTRAINT fk_user_point_log_user FOREIGN KEY (user_id) REFERENCES users (id)
);
