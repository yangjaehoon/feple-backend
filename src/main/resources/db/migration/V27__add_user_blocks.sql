CREATE TABLE user_block (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    blocker_id  BIGINT       NOT NULL,
    blocked_id  BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_block (blocker_id, blocked_id),
    INDEX idx_user_block_blocker_id (blocker_id),

    CONSTRAINT fk_user_block_blocker FOREIGN KEY (blocker_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_block_blocked FOREIGN KEY (blocked_id) REFERENCES users (id) ON DELETE CASCADE
);
