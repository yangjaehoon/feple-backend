-- 고아 스키마 추가 정리 (users.password와 같은 계열 — Java 코드가 더 이상 참조하지 않는 컬럼/테이블).
-- V1 baseline에도 없는, 운영 DB 전용 드리프트라 CI가 새로 재생하는 테스트 DB에는 애초에
-- 존재하지 않을 수 있어 전부 조건부로 처리한다.

-- festival.image_key/source_site/source_url: 과거 페스티벌 스크래퍼가 쓰던 컬럼으로 보이나,
-- 현재 스크래퍼(FestivalPageScraper)는 ScrapedFestivalDto로 결과를 반환해 관리자가 검토 후
-- 정식 Festival로 저장하는 구조라 이 세 컬럼을 읽거나 쓰는 코드가 전혀 없다(JPA/JDBC 어디에도
-- 참조 없음 확인). Festival.posterKey(=poster_key)가 실제 이미지 컬럼으로 쓰이고 있다.
SET @col_exists = (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'festival' AND column_name = 'image_key');
SET @drop_sql = IF(@col_exists > 0, 'ALTER TABLE festival DROP COLUMN image_key', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'festival' AND column_name = 'source_site');
SET @drop_sql = IF(@col_exists > 0, 'ALTER TABLE festival DROP COLUMN source_site', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'festival' AND column_name = 'source_url');
SET @drop_sql = IF(@col_exists > 0, 'ALTER TABLE festival DROP COLUMN source_url', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- password_reset_tokens: users.password와 마찬가지로 과거 이메일/비밀번호 로그인 기능의
-- 잔재. 이 테이블을 참조하는 엔티티/리포지토리/서비스가 코드베이스에 전혀 없다(현재 인증은
-- Kakao/Firebase OAuth 전용). 테이블 자체가 어떤 마이그레이션에도 없던 드리프트라
-- DROP TABLE IF EXISTS로 제거(테이블은 MySQL이 IF EXISTS를 지원해 컬럼과 달리 그대로 사용 가능).
DROP TABLE IF EXISTS password_reset_tokens;
