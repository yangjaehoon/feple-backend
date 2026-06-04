-- NONE → ALL_AGES 데이터 마이그레이션 및 컬럼 기본값 변경
UPDATE festival SET age_restriction = 'ALL_AGES' WHERE age_restriction = 'NONE';
ALTER TABLE festival ALTER COLUMN age_restriction SET DEFAULT 'ALL_AGES';
