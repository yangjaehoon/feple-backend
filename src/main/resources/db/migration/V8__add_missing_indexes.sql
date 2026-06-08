-- UNIQUE KEY의 후행 컬럼 단독 조회/삭제에 인덱스가 없어 full scan 발생.
-- 특히 게시글/댓글 삭제 시 CASCADE DELETE 경로에서 빈번히 사용됨.

-- ── post_like: deleteByPostId / deleteByPostIds ─────────────────────────────
-- UNIQUE KEY (user_id, post_id) → post_id 단독 조회 불가
ALTER TABLE `post_like`
    ADD INDEX `idx_post_like_post_id` (`post_id`);

-- ── post_scrap: deleteByPostId ──────────────────────────────────────────────
-- UNIQUE KEY (user_id, post_id) → post_id 단독 조회 불가
ALTER TABLE `post_scrap`
    ADD INDEX `idx_post_scrap_post_id` (`post_id`);

-- ── festival_like: deleteByFestivalId ──────────────────────────────────────
-- UNIQUE KEY (user_id, festival_id) → festival_id 단독 조회 불가
ALTER TABLE `festival_like`
    ADD INDEX `idx_festival_like_festival_id` (`festival_id`);

-- ── festival_attendance: deleteByFestivalId ────────────────────────────────
-- UNIQUE KEY (user_id, festival_id) → festival_id 단독 조회 불가
ALTER TABLE `festival_attendance`
    ADD INDEX `idx_festival_attendance_festival_id` (`festival_id`);

-- ── comment_like: deleteByCommentId ────────────────────────────────────────
-- UNIQUE KEY (user_id, comment_id) → comment_id 단독 조회 불가
ALTER TABLE `comment_like`
    ADD INDEX `idx_comment_like_comment_id` (`comment_id`);

-- ── post_report: findByPostId / deleteByPostId ─────────────────────────────
-- UNIQUE KEY (reporter_id, post_id) → post_id 단독 조회 불가
ALTER TABLE `post_report`
    ADD INDEX `idx_post_report_post_id` (`post_id`);

-- post_report 관리자 쿼리: findByStatusOrderByCreatedAtDesc / searchByKeyword
-- WHERE status = ? ORDER BY created_at DESC — 현재 인덱스 없음
ALTER TABLE `post_report`
    ADD INDEX `idx_post_report_status_created_at` (`status`, `created_at` DESC);

-- ── comment_report: deleteByCommentId ─────────────────────────────────────
-- UNIQUE KEY (reporter_id, comment_id) → comment_id 단독 조회 불가
ALTER TABLE `comment_report`
    ADD INDEX `idx_comment_report_comment_id` (`comment_id`);

-- comment_report 관리자 쿼리: findByStatusOrderByCreatedAtDesc / searchByKeyword
ALTER TABLE `comment_report`
    ADD INDEX `idx_comment_report_status_created_at` (`status`, `created_at` DESC);

-- ── user_device_tokens: deleteByTokenAndOtherUsers / deleteByTokenIn ───────
-- UNIQUE KEY (user_id, token) → token 단독 조회 불가
-- FCM 인증 실패 토큰 정리 시 사용
ALTER TABLE `user_device_tokens`
    ADD INDEX `idx_user_device_tokens_token` (`token`(255));

-- ── refresh_tokens: deleteExpiredTokens ────────────────────────────────────
-- WHERE expires_at < :now — 스케줄러 실행 시마다 full scan
ALTER TABLE `refresh_tokens`
    ADD INDEX `idx_refresh_tokens_expires_at` (`expires_at`);
