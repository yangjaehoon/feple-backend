-- 컬럼을 VARCHAR(20)으로 확장하여 ALL_AGES 값 수용 보장
ALTER TABLE festival MODIFY COLUMN age_restriction VARCHAR(20) DEFAULT 'ALL_AGES';
-- NONE → ALL_AGES 데이터 마이그레이션
UPDATE festival SET age_restriction = 'ALL_AGES' WHERE age_restriction = 'NONE';
