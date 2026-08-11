-- DBA 리뷰에서 발견된 스키마 위생 문제 정리 (기능 오류는 아니지만 방치 시 위험/낭비)

-- 1) 중복 UNIQUE 인덱스 제거 — 운영 DB에만 남아있던 해시 이름(UKxxx) 인덱스로, 어떤
--    Flyway 마이그레이션도 이걸 만든 적이 없다(과거 ddl-auto 시절 흔적으로 추정). 그래서
--    CI가 V1부터 새로 재생하는 테스트 DB에는 애초에 존재하지 않아 무조건 DROP하면 실패한다
--    — information_schema로 존재를 확인한 뒤에만 조건부로 지운다.
SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'certification_review_like' AND index_name = 'UKkk9g40eu34xmu16pcjf9dk3hc');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE certification_review_like DROP INDEX UKkk9g40eu34xmu16pcjf9dk3hc', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'certification_review_like' AND index_name = 'UKrsgda85maepdtslk8uayfeko3');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE certification_review_like DROP INDEX UKrsgda85maepdtslk8uayfeko3', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'song' AND index_name = 'UKj562it5ah9qjh344is8ax21ke');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE song DROP INDEX UKj562it5ah9qjh344is8ax21ke', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'user_block' AND index_name = 'UKtn2g2sexr2nbc612n8714c9mw');
SET @drop_sql = IF(@idx_exists > 0, 'ALTER TABLE user_block DROP INDEX UKtn2g2sexr2nbc612n8714c9mw', 'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) festival_genres에 PRIMARY KEY가 없어 InnoDB가 내부 clustering key로 대체하고 있었다.
--    자매 테이블 artist_genres와 동일하게 자연키를 PK로 승격 (기존 UNIQUE 제약이 이미
--    (festival_id, genres) 조합의 유일성을 보장하고 있어 안전).
ALTER TABLE festival_genres ADD PRIMARY KEY (festival_id, genres);

-- 3) users.password는 2026-08-10 커밋(마이페이지 코드스멜 정리)에서 User 엔티티 필드가
--    삭제됐지만 DB 컬럼은 남아있던 고아 컬럼. 코드 어디에서도 더 이상 참조하지 않고,
--    레거시 이메일/비밀번호 로그인 계정들의 bcrypt 해시만 무의미하게 남아있어 제거한다.
ALTER TABLE users DROP COLUMN password;

-- 4) admin_accounts.role만 네이티브 ENUM으로 남아있어 다른 role/provider류 컬럼과
--    관례가 어긋나고, 향후 관리자 역할이 추가되면 이번 report/suggestion enum drift와
--    동일한 사고가 재현될 수 있다. users.role과 동일하게 VARCHAR로 통일.
ALTER TABLE admin_accounts MODIFY COLUMN role VARCHAR(20) NOT NULL;
