-- 관리자 페이지 접근 권한을 READ / WRITE 레벨로 구분하기 위해 admin_account_permissions에
-- permission_level 컬럼 추가.
--
-- 기존 행은 과거 의미(권한 보유 = 해당 영역 전체 접근)를 유지하도록 WRITE로 백필한다.
-- DEFAULT 'WRITE'로 기존 행이 자동 채워지며, 이후 애플리케이션(JPA)은 항상 값을 명시해 저장한다.
--
-- V1 baseline이 만드는 CI 테스트 DB에는 이 컬럼이 없고, 운영 DB에 이미 있을 수도 있어(baseline drift)
-- information_schema로 조건부 추가한다.

SET @col_exists = (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'admin_account_permissions'
      AND column_name = 'permission_level');
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE admin_account_permissions ADD COLUMN permission_level VARCHAR(10) NOT NULL DEFAULT ''WRITE''',
    'DO 0');
PREPARE stmt FROM @add_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
