-- FK 컬럼 단독 조회/삭제에 인덱스가 없어 full scan 발생하는 테이블들

-- ── song.artist_id ──────────────────────────────────────────────────────────
-- UNIQUE KEY (youtube_video_id, artist_id) 선두 컬럼이 youtube_video_id라
-- findByArtistIdOrderByIdDesc / deleteByArtistId 쿼리가 full scan
ALTER TABLE `song`
    ADD INDEX `idx_song_artist_id` (`artist_id`);

-- ── stage.festival_id ────────────────────────────────────────────────────────
-- findByFestivalIdOrderByDisplayOrder / deleteByFestivalId full scan
ALTER TABLE `stage`
    ADD INDEX `idx_stage_festival_id` (`festival_id`);

-- ── booth.festival_id ────────────────────────────────────────────────────────
-- findByFestivalId / deleteByFestivalId full scan
ALTER TABLE `booth`
    ADD INDEX `idx_booth_festival_id` (`festival_id`);

-- ── timetable_entry.festival_id ──────────────────────────────────────────────
-- findByFestivalIdWithStage / deleteByFestivalId / nullifyArtistId full scan
ALTER TABLE `timetable_entry`
    ADD INDEX `idx_timetable_entry_festival_id` (`festival_id`);

-- ── artist_suggestion: user_id 및 status/created_at ─────────────────────────
-- deleteByUserId (cascade delete), findByStatus (관리자 목록) full scan
ALTER TABLE `artist_suggestion`
    ADD INDEX `idx_artist_suggestion_user_id` (`user_id`),
    ADD INDEX `idx_artist_suggestion_status_created_at` (`status`, `created_at` DESC);

-- ── notifications: festival_id, post_id ─────────────────────────────────────
-- deleteByFestivalId / deleteByPostIdIn full scan
-- 현재 인덱스: (user_id, created_at DESC) 만 존재
ALTER TABLE `notifications`
    ADD INDEX `idx_notifications_festival_id` (`festival_id`),
    ADD INDEX `idx_notifications_post_id` (`post_id`);

-- ── artist_festival_song.artist_festival_id ──────────────────────────────────
-- UNIQUE KEY (song_id, artist_festival_id) 선두 컬럼이 song_id라
-- deleteByArtistFestivalId / deleteByArtistFestivalIdIn / deleteByFestivalId full scan
ALTER TABLE `artist_festival_song`
    ADD INDEX `idx_afs_artist_festival_id` (`artist_festival_id`);

-- ── song_request: artist_id, user_id ────────────────────────────────────────
-- deleteByArtistId / findByArtistIdAndStatus full scan
-- deleteByUserId / findByArtistIdAndUserId full scan
-- status/created_at 인덱스는 V9에서 추가됨
ALTER TABLE `song_request`
    ADD INDEX `idx_song_request_artist_id` (`artist_id`),
    ADD INDEX `idx_song_request_user_id` (`user_id`);
