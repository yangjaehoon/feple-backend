-- V64와 같은 계열의 추가 발견: artist_festival(2건)/festival_like/post_like/timetable_entry에
-- FK 제약이 (Flyway 도입 이전 ddl-auto 시절에) 자동 생성한 해시 이름 인덱스가 남아있는데,
-- 정작 그 컬럼은 V1 baseline이 만든 의미 있는 이름의 수동 인덱스가 이미 커버하고 있어
-- 완전히 중복이다. 다른 인덱스가 해당 FK의 선두 컬럼을 계속 커버하므로 FK 제약 자체는
-- 그대로 유지된 채 안전하게 제거 가능. V64와 동일하게, CI가 V1부터 새로 재생하는 테스트
-- DB에는 이 해시 인덱스가 애초에 없으므로 조건부로만 지운다.
SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'artist_festival' AND index_name = 'FKb1wyl3ds03msu9e21ctp60lal');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE artist_festival DROP INDEX FKb1wyl3ds03msu9e21ctp60lal', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'artist_festival' AND index_name = 'FKfw3dqqb292lihhf5dxrb9ivk5');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE artist_festival DROP INDEX FKfw3dqqb292lihhf5dxrb9ivk5', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'festival_like' AND index_name = 'FKsvsiclbp7p0jhuq5vmb3r14y');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE festival_like DROP INDEX FKsvsiclbp7p0jhuq5vmb3r14y', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'post_like' AND index_name = 'FKj7iy0k7n3d0vkh8o7ibjna884');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE post_like DROP INDEX FKj7iy0k7n3d0vkh8o7ibjna884', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'timetable_entry' AND index_name = 'FK5jbc48vfm3byfn4in5npf5ne3');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE timetable_entry DROP INDEX FK5jbc48vfm3byfn4in5npf5ne3', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
