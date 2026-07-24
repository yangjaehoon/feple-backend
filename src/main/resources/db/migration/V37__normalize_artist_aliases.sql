-- artist.aliases (VARCHAR 500, 쉼표 구분) → artist_aliases 정규화 테이블
-- 1NF: 한 컬럼에 여러 값(비원자적) 위반 수정

CREATE TABLE artist_aliases (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    artist_id BIGINT       NOT NULL,
    alias     VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_artist_alias (artist_id, alias),
    KEY idx_artist_alias_artist_id (artist_id),
    FULLTEXT INDEX ft_artist_alias (alias) WITH PARSER ngram,
    CONSTRAINT fk_artist_alias FOREIGN KEY (artist_id) REFERENCES artist (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 쉼표 구분 aliases를 행 단위로 마이그레이션
INSERT INTO artist_aliases (artist_id, alias)
SELECT a.id, TRIM(jt.alias_val)
FROM artist a,
     JSON_TABLE(
         CONCAT('["', REPLACE(REPLACE(TRIM(a.aliases), '"', '\\"'), ',', '","'), '"]'),
         '$[*]' COLUMNS (alias_val VARCHAR(200) PATH '$')
     ) AS jt
WHERE a.aliases IS NOT NULL
  AND TRIM(a.aliases) != ''
  AND TRIM(jt.alias_val) != '';

-- aliases 컬럼 포함 FULLTEXT 인덱스 제거 후 컬럼 삭제
ALTER TABLE artist DROP INDEX ft_artist_name;
ALTER TABLE artist DROP COLUMN aliases;

-- name, name_en 전용 FULLTEXT 재생성 (alias 검색은 artist_aliases.ft_artist_alias 사용)
ALTER TABLE artist ADD FULLTEXT INDEX ft_artist_name (name, name_en) WITH PARSER ngram;
