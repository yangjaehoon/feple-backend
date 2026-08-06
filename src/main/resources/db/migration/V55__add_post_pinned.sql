-- 게시판 공지/고정글 지원
ALTER TABLE `post`
    ADD COLUMN `pinned` TINYINT(1) NOT NULL DEFAULT 0 AFTER `anonymous`;

ALTER TABLE `post`
    ADD INDEX `idx_post_pinned_board_type` (`pinned`, `board_type`, `created_at` DESC);
