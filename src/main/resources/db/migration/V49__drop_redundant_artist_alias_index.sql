-- uq_artist_alias(artist_id, alias)가 이미 artist_id 단독 조회를 커버하므로
-- idx_artist_alias_artist_id는 쓰기 오버헤드만 유발하는 중복 인덱스
ALTER TABLE artist_aliases DROP INDEX idx_artist_alias_artist_id;
