CREATE TABLE setlist_change_requests (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    festival_id          BIGINT        NOT NULL,
    artist_festival_id   BIGINT        NOT NULL,
    artist_name          VARCHAR(100)  NOT NULL,
    festival_title       VARCHAR(200)  NOT NULL,
    message    TEXT          NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_setlist_req_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
