-- Flyway baseline for existing production schema.
-- New environments run this to bootstrap the DB from scratch.
-- Existing environments are marked at version 1 via baseline-on-migrate.

-- ============================================================
-- users
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `nickname`          VARCHAR(255)    NULL,
    `oauth_id`          VARCHAR(255)    NOT NULL,
    `provider`          VARCHAR(255)    NULL,
    `profile_image_url` VARCHAR(255)    NULL,
    `email`             VARCHAR(255)    NULL,
    `password`          VARCHAR(255)    NULL,
    `role`              VARCHAR(255)    NOT NULL DEFAULT 'USER',
    `bio`               VARCHAR(150)    NULL,
    `created_at`        DATETIME(6)     NULL,
    `banned_until`      DATETIME(6)     NULL,
    `ban_reason`        VARCHAR(300)    NULL,
    `banned_by`         VARCHAR(100)    NULL,
    `deleted_at`        DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_users_provider_oauth_id` (`provider`, `oauth_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `name`              VARCHAR(255)    NULL,
    `name_en`           VARCHAR(255)    NULL,
    `genre`             VARCHAR(255)    NULL,
    `profile_image_key` VARCHAR(255)    NULL,
    `follower_count`    INT             NOT NULL DEFAULT 0,
    `weekly_score`      INT             NOT NULL DEFAULT 0,
    `rank_updated_at`   DATETIME(6)     NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- festival
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `title`           VARCHAR(255)    NOT NULL,
    `title_en`        VARCHAR(255)    NULL,
    `description`     VARCHAR(1000)   NULL,
    `location`        VARCHAR(255)    NULL,
    `start_date`      DATE            NULL,
    `end_date`        DATE            NULL,
    `poster_key`      VARCHAR(255)    NULL,
    `like_count`      INT             NOT NULL DEFAULT 0,
    `attending_count` INT             NOT NULL DEFAULT 0,
    `event_type`      VARCHAR(255)    NULL     DEFAULT 'FESTIVAL',
    `region`          VARCHAR(20)     NULL,
    `age_restriction` VARCHAR(10)     NULL     DEFAULT 'NONE',
    `latitude`        DOUBLE          NULL,
    `longitude`       DOUBLE          NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `festival_genres` (
    `festival_id` BIGINT       NOT NULL,
    `genres`      VARCHAR(255) NULL,
    CONSTRAINT `fk_fg_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_festival (depends on artist, festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_festival` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `artist_id`    BIGINT          NULL,
    `festival_id`  BIGINT          NULL,
    `lineup_order` INT             NULL,
    `stage_name`   VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_af_artist_festival` (`artist_id`, `festival_id`),
    KEY `idx_af_artist_id`   (`artist_id`),
    KEY `idx_af_festival_id` (`festival_id`),
    CONSTRAINT `fk_af_artist`   FOREIGN KEY (`artist_id`)   REFERENCES `artist`   (`id`),
    CONSTRAINT `fk_af_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- song (depends on artist)
-- ============================================================
CREATE TABLE IF NOT EXISTS `song` (
    `id`               BIGINT          NOT NULL AUTO_INCREMENT,
    `title`            VARCHAR(255)    NOT NULL,
    `youtube_video_id` VARCHAR(255)    NOT NULL,
    `thumbnail_url`    VARCHAR(255)    NULL,
    `artist_id`        BIGINT          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_song_ytid_artist` (`youtube_video_id`, `artist_id`),
    CONSTRAINT `fk_song_artist` FOREIGN KEY (`artist_id`) REFERENCES `artist` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_festival_song (depends on song, artist_festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_festival_song` (
    `id`                 BIGINT NOT NULL AUTO_INCREMENT,
    `song_id`            BIGINT NOT NULL,
    `artist_festival_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_afs_song_artist_festival` (`song_id`, `artist_festival_id`),
    CONSTRAINT `fk_afs_song`            FOREIGN KEY (`song_id`)            REFERENCES `song`            (`id`),
    CONSTRAINT `fk_afs_artist_festival` FOREIGN KEY (`artist_festival_id`) REFERENCES `artist_festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- stage (depends on festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `stage` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `festival_id`   BIGINT          NOT NULL,
    `name`          VARCHAR(255)    NOT NULL,
    `display_order` INT             NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_stage_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- timetable_entry (depends on festival, stage, artist)
-- ============================================================
CREATE TABLE IF NOT EXISTS `timetable_entry` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `festival_id`   BIGINT       NOT NULL,
    `stage_id`      BIGINT       NULL,
    `artist_id`     BIGINT       NULL,
    `artist_name`   VARCHAR(255) NOT NULL,
    `festival_date` DATE         NOT NULL,
    `start_time`    TIME         NOT NULL,
    `end_time`      TIME         NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_te_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`),
    CONSTRAINT `fk_te_stage`    FOREIGN KEY (`stage_id`)    REFERENCES `stage`    (`id`),
    CONSTRAINT `fk_te_artist`   FOREIGN KEY (`artist_id`)   REFERENCES `artist`   (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- booth (depends on festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `booth` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `festival_id` BIGINT          NOT NULL,
    `name`        VARCHAR(255)    NOT NULL,
    `booth_type`  VARCHAR(255)    NOT NULL,
    `latitude`    DOUBLE          NOT NULL,
    `longitude`   DOUBLE          NOT NULL,
    `description` VARCHAR(255)    NULL,
    `image_url`   VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_booth_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_photos (depends on artist, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_photos` (
    `id`               BIGINT          NOT NULL AUTO_INCREMENT,
    `artist_id`        BIGINT          NOT NULL,
    `uploader_user_id` BIGINT          NOT NULL,
    `s3_key`           VARCHAR(500)    NOT NULL,
    `content_type`     VARCHAR(100)    NOT NULL,
    `created_at`       DATETIME(6)     NOT NULL,
    `title`            VARCHAR(100)    NOT NULL,
    `description`      TEXT            NULL,
    `like_count`       INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_gallery_photo_artist_id`   (`artist_id`),
    KEY `idx_gallery_photo_uploader_id` (`uploader_user_id`),
    CONSTRAINT `fk_ap_artist`   FOREIGN KEY (`artist_id`)        REFERENCES `artist` (`id`),
    CONSTRAINT `fk_ap_uploader` FOREIGN KEY (`uploader_user_id`) REFERENCES `users`  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_photo_likes (depends on artist_photos, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_photo_likes` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `artist_photo_id` BIGINT      NOT NULL,
    `user_id`         BIGINT      NOT NULL,
    `created_at`      DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_apl_photo_user` (`artist_photo_id`, `user_id`),
    CONSTRAINT `fk_apl_photo` FOREIGN KEY (`artist_photo_id`) REFERENCES `artist_photos` (`id`),
    CONSTRAINT `fk_apl_user`  FOREIGN KEY (`user_id`)         REFERENCES `users`         (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_photo_report (depends on artist_photos, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_photo_report` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `photo_id`    BIGINT          NOT NULL,
    `reporter_id` BIGINT          NOT NULL,
    `reason`      VARCHAR(255)    NOT NULL,
    `status`      VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
    `detail`      VARCHAR(255)    NULL,
    `created_at`  DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_apr_reporter_photo` (`reporter_id`, `photo_id`),
    CONSTRAINT `fk_apr_photo`    FOREIGN KEY (`photo_id`)    REFERENCES `artist_photos` (`id`),
    CONSTRAINT `fk_apr_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users`         (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_image (depends on artist, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_image` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `image_url`   VARCHAR(255) NULL,
    `artist_id`   BIGINT       NULL,
    `uploader_id` BIGINT       NULL,
    `upload_at`   DATETIME(6)  NULL,
    `like_count`  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ai_artist`   FOREIGN KEY (`artist_id`)   REFERENCES `artist` (`id`),
    CONSTRAINT `fk_ai_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `users`  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_image_like (depends on artist_image, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_image_like` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT NULL,
    `artist_image_id` BIGINT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ail_user_image` (`user_id`, `artist_image_id`),
    CONSTRAINT `fk_ail_user`  FOREIGN KEY (`user_id`)         REFERENCES `users`        (`id`),
    CONSTRAINT `fk_ail_image` FOREIGN KEY (`artist_image_id`) REFERENCES `artist_image` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_follow (depends on users, artist)
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_follow` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NULL,
    `user_id`    BIGINT      NOT NULL,
    `artist_id`  BIGINT      NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_afol_user_artist` (`user_id`, `artist_id`),
    KEY `idx_artist_follow_artist_id_created_at` (`artist_id`, `created_at`),
    CONSTRAINT `fk_afol_user`   FOREIGN KEY (`user_id`)   REFERENCES `users`  (`id`),
    CONSTRAINT `fk_afol_artist` FOREIGN KEY (`artist_id`) REFERENCES `artist` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- festival_attendance (depends on users, festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival_attendance` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `festival_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fa_user_festival` (`user_id`, `festival_id`),
    CONSTRAINT `fk_fa_user`     FOREIGN KEY (`user_id`)     REFERENCES `users`    (`id`),
    CONSTRAINT `fk_fa_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- festival_like (depends on users, festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival_like` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `festival_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fl_user_festival` (`user_id`, `festival_id`),
    CONSTRAINT `fk_fl_user`     FOREIGN KEY (`user_id`)     REFERENCES `users`    (`id`),
    CONSTRAINT `fk_fl_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- festival_weather (depends on festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival_weather` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `festival_id` BIGINT          NOT NULL,
    `fcst_date`   DATE            NULL,
    `min_temp`    DOUBLE          NULL,
    `max_temp`    DOUBLE          NULL,
    `rain_prob`   INT             NULL,
    `sky_code`    VARCHAR(255)    NULL,
    `pty_code`    VARCHAR(255)    NULL,
    `saved_at`    DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fw_festival` (`festival_id`),
    CONSTRAINT `fk_fw_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- festival_certification (depends on users, festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival_certification` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT          NOT NULL,
    `festival_id`       BIGINT          NOT NULL,
    `photo_key`         VARCHAR(255)    NOT NULL,
    `status`            VARCHAR(255)    NOT NULL,
    `rejection_message` VARCHAR(500)    NULL,
    `created_at`        DATETIME(6)     NOT NULL,
    `reviewed_at`       DATETIME(6)     NULL,
    `reviewed_by`       VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fc_user_festival` (`user_id`, `festival_id`),
    CONSTRAINT `fk_fc_user`     FOREIGN KEY (`user_id`)     REFERENCES `users`    (`id`),
    CONSTRAINT `fk_festcert_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- post (depends on users, artist, festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `post` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `title`         VARCHAR(255)  NULL,
    `content`       TEXT          NULL,
    `board_type`    VARCHAR(255)  NULL,
    `like_count`    INT           NOT NULL DEFAULT 0,
    `scrap_count`   INT           NOT NULL DEFAULT 0,
    `image_url`     VARCHAR(255)  NULL,
    `created_at`    DATETIME(6)   NOT NULL,
    `updated_at`    DATETIME(6)   NOT NULL,
    `deleted_at`    DATETIME(6)   NULL,
    `anonymous`     TINYINT(1)    NOT NULL DEFAULT 0,
    `view_count`    INT           NOT NULL DEFAULT 0,
    `comment_count` INT           NOT NULL DEFAULT 0,
    `user_id`       BIGINT        NOT NULL,
    `artist_id`     BIGINT        NULL,
    `festival_id`   BIGINT        NULL,
    PRIMARY KEY (`id`),
    KEY `idx_post_board_type_created_at`  (`board_type`, `created_at` DESC),
    KEY `idx_post_like_count_created_at`  (`like_count` DESC, `created_at` DESC),
    KEY `idx_post_board_type_id`          (`board_type`, `id` DESC),
    KEY `idx_post_artist_id_created_at`   (`artist_id`, `created_at` DESC),
    KEY `idx_post_festival_id_created_at` (`festival_id`, `created_at` DESC),
    KEY `idx_post_user_id_created_at`     (`user_id`, `created_at` DESC),
    CONSTRAINT `fk_post_user`     FOREIGN KEY (`user_id`)     REFERENCES `users`    (`id`),
    CONSTRAINT `fk_post_artist`   FOREIGN KEY (`artist_id`)   REFERENCES `artist`   (`id`),
    CONSTRAINT `fk_post_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- post_like (depends on users, post)
-- ============================================================
CREATE TABLE IF NOT EXISTS `post_like` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `post_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_pl_user_post` (`user_id`, `post_id`),
    CONSTRAINT `fk_pl_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_pl_post` FOREIGN KEY (`post_id`) REFERENCES `post`  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- post_report (depends on users, post)
-- ============================================================
CREATE TABLE IF NOT EXISTS `post_report` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `post_id`     BIGINT          NOT NULL,
    `reporter_id` BIGINT          NOT NULL,
    `reason`      VARCHAR(255)    NOT NULL,
    `status`      VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
    `detail`      VARCHAR(255)    NULL,
    `created_at`  DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_pr_reporter_post` (`reporter_id`, `post_id`),
    CONSTRAINT `fk_pr_post`     FOREIGN KEY (`post_id`)     REFERENCES `post`  (`id`),
    CONSTRAINT `fk_pr_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- post_scrap (depends on users, post)
-- ============================================================
CREATE TABLE IF NOT EXISTS `post_scrap` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `post_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ps_user_post` (`user_id`, `post_id`),
    CONSTRAINT `fk_ps_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_ps_post` FOREIGN KEY (`post_id`) REFERENCES `post`  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- comment (depends on post, users; self-referencing for parent)
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `content`    TEXT        NULL,
    `created_at` DATETIME(6) NULL,
    `updated_at` DATETIME(6) NULL,
    `deleted_at` DATETIME(6) NULL,
    `post_id`    BIGINT      NOT NULL,
    `user_id`    BIGINT      NOT NULL,
    `parent_id`  BIGINT      NULL,
    `like_count` INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_comment_post_id_created_at` (`post_id`, `created_at` ASC),
    KEY `idx_comment_user_id_created_at` (`user_id`, `created_at` DESC),
    CONSTRAINT `fk_comment_post`   FOREIGN KEY (`post_id`)   REFERENCES `post`    (`id`),
    CONSTRAINT `fk_comment_user`   FOREIGN KEY (`user_id`)   REFERENCES `users`   (`id`),
    CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- comment_like (depends on comment, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT NOT NULL,
    `user_id`    BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_cl_user_comment` (`user_id`, `comment_id`),
    CONSTRAINT `fk_cl_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`),
    CONSTRAINT `fk_cl_user`    FOREIGN KEY (`user_id`)    REFERENCES `users`   (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- comment_report (depends on comment, users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment_report` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `comment_id`  BIGINT          NOT NULL,
    `reporter_id` BIGINT          NOT NULL,
    `reason`      VARCHAR(255)    NOT NULL,
    `status`      VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
    `detail`      VARCHAR(255)    NULL,
    `created_at`  DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_cr_reporter_comment` (`reporter_id`, `comment_id`),
    CONSTRAINT `fk_cr_comment`  FOREIGN KEY (`comment_id`)  REFERENCES `comment` (`id`),
    CONSTRAINT `fk_cr_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users`   (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- notifications (depends on users, festival, post)
-- ============================================================
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT          NOT NULL,
    `type`        VARCHAR(30)     NOT NULL,
    `title`       VARCHAR(100)    NOT NULL,
    `body`        VARCHAR(255)    NOT NULL,
    `festival_id` BIGINT          NULL,
    `post_id`     BIGINT          NULL,
    `is_read`     TINYINT(1)      NOT NULL DEFAULT 0,
    `created_at`  DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_id_created_at` (`user_id`, `created_at` DESC),
    CONSTRAINT `fk_notif_user`     FOREIGN KEY (`user_id`)     REFERENCES `users`    (`id`),
    CONSTRAINT `fk_notif_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`),
    CONSTRAINT `fk_notif_post`     FOREIGN KEY (`post_id`)     REFERENCES `post`     (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- notification_preferences
-- ============================================================
CREATE TABLE IF NOT EXISTS `notification_preferences` (
    `id`                   BIGINT     NOT NULL AUTO_INCREMENT,
    `user_id`              BIGINT     NOT NULL,
    `cert_enabled`         TINYINT(1) NOT NULL DEFAULT 1,
    `comment_enabled`      TINYINT(1) NOT NULL DEFAULT 1,
    `festival_enabled`     TINYINT(1) NOT NULL DEFAULT 1,
    `song_request_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_np_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- broadcast_notifications
-- ============================================================
CREATE TABLE IF NOT EXISTS `broadcast_notifications` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(100)    NOT NULL,
    `body`       VARCHAR(500)    NOT NULL,
    `created_at` DATETIME(6)     NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- search_log
-- ============================================================
CREATE TABLE IF NOT EXISTS `search_log` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `keyword`    VARCHAR(200)    NOT NULL,
    `created_at` DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_search_log_created_at` (`created_at`),
    KEY `idx_search_log_keyword`    (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- bad_words
-- ============================================================
CREATE TABLE IF NOT EXISTS `bad_words` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `word`       VARCHAR(50) NOT NULL,
    `created_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_bad_words_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- artist_suggestion
-- ============================================================
CREATE TABLE IF NOT EXISTS `artist_suggestion` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT          NOT NULL,
    `artist_name`  VARCHAR(255)    NOT NULL,
    `note`         VARCHAR(255)    NULL,
    `process_note` VARCHAR(500)    NULL,
    `status`       VARCHAR(255)    NOT NULL,
    `created_at`   DATETIME(6)     NULL,
    `processed_at` DATETIME(6)     NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- song_request (depends on artist)
-- ============================================================
CREATE TABLE IF NOT EXISTS `song_request` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `artist_id`   BIGINT          NOT NULL,
    `user_id`     BIGINT          NOT NULL,
    `song_title`  VARCHAR(255)    NOT NULL,
    `youtube_url` VARCHAR(255)    NULL,
    `status`      VARCHAR(255)    NOT NULL,
    `created_at`  DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_sr_artist` FOREIGN KEY (`artist_id`) REFERENCES `artist` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- refresh_tokens (depends on users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT          NOT NULL,
    `token_hash` VARCHAR(64)     NOT NULL,
    `expires_at` DATETIME(6)     NOT NULL,
    `created_at` DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_user_id`    (`user_id`),
    CONSTRAINT `fk_rt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- user_device_tokens (depends on users)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_device_tokens` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT          NOT NULL,
    `token`      VARCHAR(512)    NOT NULL,
    `platform`   VARCHAR(10)     NULL,
    `updated_at` DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_udt_user_token` (`user_id`, `token`),
    CONSTRAINT `fk_udt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- admin_accounts
-- ============================================================
CREATE TABLE IF NOT EXISTS `admin_accounts` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `username`          VARCHAR(50)     NOT NULL,
    `password`          VARCHAR(255)    NOT NULL,
    `display_name`      VARCHAR(50)     NULL,
    `role`              VARCHAR(20)     NOT NULL,
    `profile_image_url` VARCHAR(512)    NULL,
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`        DATETIME(6)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_aa_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_account_permissions` (
    `admin_account_id` BIGINT      NOT NULL,
    `permission`       VARCHAR(30) NULL,
    CONSTRAINT `fk_aap_account` FOREIGN KEY (`admin_account_id`) REFERENCES `admin_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- festival_checklist
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival_checklist` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `festival_id` BIGINT      NOT NULL,
    `lineup1`     TINYINT(1)  NOT NULL DEFAULT 0,
    `lineup2`     TINYINT(1)  NOT NULL DEFAULT 0,
    `lineup3`     TINYINT(1)  NOT NULL DEFAULT 0,
    `booth_map`   TINYINT(1)  NOT NULL DEFAULT 0,
    `timetable`   TINYINT(1)  NOT NULL DEFAULT 0,
    `memo`        TEXT        NULL,
    `updated_at`  DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fc_festival_id` (`festival_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- admin_logs
-- ============================================================
CREATE TABLE IF NOT EXISTS `admin_logs` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    `admin_username` VARCHAR(100)    NULL,
    `action`         VARCHAR(50)     NOT NULL,
    `target_type`    VARCHAR(30)     NULL,
    `target_id`      BIGINT          NULL,
    `detail`         VARCHAR(500)    NULL,
    `ip_address`     VARCHAR(45)     NULL,
    `created_at`     DATETIME(6)     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_admin_logs_created_at`  (`created_at`),
    KEY `idx_admin_logs_target_type` (`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- shedlock (ShedLock distributed scheduler)
-- ============================================================
CREATE TABLE IF NOT EXISTS `shedlock` (
    `name`       VARCHAR(64)  NOT NULL,
    `lock_until` TIMESTAMP(3) NOT NULL,
    `locked_at`  TIMESTAMP(3) NOT NULL,
    `locked_by`  VARCHAR(255) NOT NULL,
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
