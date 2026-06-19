-- P0: 활성 축제 필터링 (end_date >= today)
ALTER TABLE `festival`
    ADD INDEX `idx_festival_end_date` (`end_date`);

-- P1: 지역 필터 (findByFilters region IN :regions)
ALTER TABLE `festival`
    ADD INDEX `idx_festival_region` (`region`);

-- P3: upcoming 축제 정렬 (startDate BETWEEN AND ORDER BY likeCount DESC)
ALTER TABLE `festival`
    ADD INDEX `idx_festival_start_like` (`start_date`, `like_count` DESC);

-- P2: 장르 필터 (festival_genres.genre IN :genres)
ALTER TABLE `festival_genres`
    ADD INDEX `idx_festival_genres_genre` (`genre`);
