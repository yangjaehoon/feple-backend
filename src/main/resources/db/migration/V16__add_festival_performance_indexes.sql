-- P0: 활성 축제 필터링 (end_date >= today)
CREATE INDEX IF NOT EXISTS `idx_festival_end_date` ON `festival` (`end_date`);

-- P1: 지역 필터 (findByFilters region IN :regions)
CREATE INDEX IF NOT EXISTS `idx_festival_region` ON `festival` (`region`);

-- P3: upcoming 축제 정렬 (startDate BETWEEN AND ORDER BY likeCount DESC)
CREATE INDEX IF NOT EXISTS `idx_festival_start_like` ON `festival` (`start_date`, `like_count` DESC);

-- P2: 장르 필터 (festival_genres.genre IN :genres)
CREATE INDEX IF NOT EXISTS `idx_festival_genres_genre` ON `festival_genres` (`genre`);
