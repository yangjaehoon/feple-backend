-- 관리자 페이지 성능 개선을 위한 인덱스 추가

-- festival_certification: countByStatus / findByStatusOrderByCreatedAtDesc
ALTER TABLE `festival_certification`
    ADD INDEX `idx_festival_cert_status_created_at` (`status`, `created_at` DESC);

-- song_request: countByStatus / findByStatusOrderByCreatedAtDesc
ALTER TABLE `song_request`
    ADD INDEX `idx_song_request_status_created_at` (`status`, `created_at` DESC);

-- artist: findTop10ByOrderByFollowerCountDesc (대시보드 인기 아티스트)
ALTER TABLE `artist`
    ADD INDEX `idx_artist_follower_count` (`follower_count` DESC);

-- festival: findTop10ByOrderByLikeCountDesc / findUpcomingFestivalsSortedByLike
ALTER TABLE `festival`
    ADD INDEX `idx_festival_like_count` (`like_count` DESC);
ALTER TABLE `festival`
    ADD INDEX `idx_festival_start_date` (`start_date`);

-- users: countByCreatedAtBetween (통계), countByDeletedAtIsNull / findByDeletedAtIsNull (목록)
ALTER TABLE `users`
    ADD INDEX `idx_users_created_at` (`created_at`);
ALTER TABLE `users`
    ADD INDEX `idx_users_deleted_at` (`deleted_at`);
