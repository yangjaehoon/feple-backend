-- 감사 로그(admin_logs) 운영성 개선
--
-- 1) admin_username 인덱스 추가
--    감사 로그 화면(logs.html)의 "관리자 이름" 필터가 LIKE '%kw%' 리딩 와일드카드라
--    인덱스가 없으면 admin_logs가 커질수록 전체 스캔이 된다. 접두 매칭·정렬이라도
--    커버하도록 인덱스를 추가한다.
--    V1 baseline이 새로 만드는 CI 테스트 DB에는 이 인덱스가 없고, 운영 DB에 이미 있을
--    수도 있어(baseline drift) information_schema로 조건부 생성한다.
--
-- 2) detail 컬럼 폭 확장 (→ VARCHAR(2000))
--    일괄 작업(회원/게시글/신고/인증) 감사 로그가 선택된 id 목록을 그대로 detail에
--    기록하도록 바뀌면서(BULK_ACTION_MAX_IDS=50), 기존 폭으로는 잘려나갈 수 있다.
--    폭만 넓히므로 현재 타입이 무엇이든(255/500/2000) 그대로 실행해도 안전하다.

SET @idx_exists = (SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'admin_logs'
      AND index_name = 'idx_admin_logs_admin_username');
SET @create_sql = IF(@idx_exists = 0,
    'ALTER TABLE admin_logs ADD INDEX idx_admin_logs_admin_username (admin_username)',
    'DO 0');
PREPARE stmt FROM @create_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE admin_logs MODIFY COLUMN detail VARCHAR(2000) NULL;
