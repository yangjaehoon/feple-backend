-- V43에서 s3_key를 정식으로 추가하기 전, artist_photos는 Hibernate 암묵적
-- 네이밍 전략으로 만들어진 레거시 컬럼 s3key(언더스코어 없음)를 실제로 쓰고
-- 있었고, likecount(레거시)도 like_count와 별개로 남아 있었다. 엔티티는
-- s3_key/like_count만 참조하므로 두 레거시 컬럼은 죽은 데이터다.
SET @s3key_count = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'artist_photos' AND COLUMN_NAME = 's3key'
);
SET @drop_s3key = IF(@s3key_count > 0, 'ALTER TABLE `artist_photos` DROP COLUMN `s3key`', 'DO 0');
PREPARE s FROM @drop_s3key; EXECUTE s; DEALLOCATE PREPARE s;

SET @likecount_count = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'artist_photos' AND COLUMN_NAME = 'likecount'
);
SET @drop_likecount = IF(@likecount_count > 0, 'ALTER TABLE `artist_photos` DROP COLUMN `likecount`', 'DO 0');
PREPARE s FROM @drop_likecount; EXECUTE s; DEALLOCATE PREPARE s;
