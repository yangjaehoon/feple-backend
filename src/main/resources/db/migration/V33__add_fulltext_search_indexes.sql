-- 사용자 통합 검색(게시글 제목/페스티벌 제목/아티스트 이름) FULLTEXT 인덱스 추가.
-- 기존 LIKE '%keyword%'는 B-tree 인덱스를 못 타 풀스캔이었음 — 데이터가 늘면
-- 검색 API부터 느려지는 전형적 패턴. ngram 파서로 한글 토큰화를 지원한다.
-- (관리자 전용 검색·금칙어 포함 여부 카운트는 트래픽이 낮고 정확한 부분일치가
--  필요해 대상에서 제외 — LIKE 유지)

ALTER TABLE `post`     ADD FULLTEXT INDEX `ft_post_title`     (`title`) WITH PARSER ngram;
ALTER TABLE `festival` ADD FULLTEXT INDEX `ft_festival_title` (`title`) WITH PARSER ngram;
ALTER TABLE `artist`   ADD FULLTEXT INDEX `ft_artist_name`    (`name`, `name_en`, `aliases`) WITH PARSER ngram;
