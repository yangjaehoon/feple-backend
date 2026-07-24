-- artist: getAllArtists() — ORDER BY weekly_score DESC, id ASC (filesort 제거)
ALTER TABLE `artist`
    ADD INDEX `idx_artist_weekly_score` (`weekly_score` DESC, `id` ASC);
