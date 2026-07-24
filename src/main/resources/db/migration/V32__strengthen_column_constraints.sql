-- ── 1. artist_festival.artist_id / festival_id: NOT NULL 강화 ───────────────
-- 라인업 참여 레코드는 항상 아티스트·페스티벌 둘 다 있어야 함(도메인 불변식).
-- NULL 허용 시 uq_af_artist_festival(artist_id, festival_id) 유니크 제약이
-- NULL 조합에 대해서는 중복을 막지 못함(MySQL은 NULL을 서로 다른 값으로 취급).
-- 애플리케이션은 항상 값을 채워 저장하므로 고아 행(NULL)이 남아있다면 이미
-- 참여 정보로서 의미가 없는 데이터이므로 정리 후 제약을 강화한다.
DELETE FROM `artist_festival` WHERE `artist_id` IS NULL OR `festival_id` IS NULL;

ALTER TABLE `artist_festival`
    MODIFY COLUMN `artist_id`   BIGINT NOT NULL,
    MODIFY COLUMN `festival_id` BIGINT NOT NULL;

-- ── 2. 이미지/파일 키 컬럼 길이 통일 (255 → 500) ─────────────────────────────
-- artist_photos.s3_key만 500자였고 나머지 키/URL 컬럼은 255자로 남아있어
-- S3 키 네이밍이나 CDN URL이 길어지면 다른 컬럼에서만 조용히 잘릴 수 있었음.
ALTER TABLE `festival` MODIFY COLUMN `poster_key` VARCHAR(500) NULL;
ALTER TABLE `users`    MODIFY COLUMN `profile_image_url` VARCHAR(500) NULL;
ALTER TABLE `artist`   MODIFY COLUMN `profile_image_key` VARCHAR(500) NULL;
ALTER TABLE `booth`    MODIFY COLUMN `image_url` VARCHAR(500) NULL;
