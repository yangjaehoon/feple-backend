-- artist.genre: List<ArtistGenre>을 ArtistGenreConverter로 콤마 구분 문자열("BAND,HIP_HOP")로
-- 단일 컬럼에 저장 — 비원자값(1NF 위반). artist_aliases 정규화(V37)와 동일 방식으로 분리.

CREATE TABLE artist_genres (
    artist_id BIGINT      NOT NULL,
    genres    VARCHAR(20) NOT NULL,
    PRIMARY KEY (artist_id, genres),
    CONSTRAINT fk_ag_artist FOREIGN KEY (artist_id) REFERENCES artist (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO artist_genres (artist_id, genres)
SELECT a.id, TRIM(jt.genre_val)
FROM artist a,
     JSON_TABLE(
         CONCAT('["', REPLACE(REPLACE(TRIM(a.genre), '"', '\\"'), ',', '","'), '"]'),
         '$[*]' COLUMNS (genre_val VARCHAR(20) PATH '$')
     ) AS jt
WHERE a.genre IS NOT NULL
  AND TRIM(a.genre) != ''
  AND TRIM(jt.genre_val) != '';

ALTER TABLE artist DROP COLUMN genre;
