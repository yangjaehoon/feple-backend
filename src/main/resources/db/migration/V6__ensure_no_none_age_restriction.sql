-- V5 마이그레이션이 부분 실행된 경우를 대비해 남아있는 NONE 값 처리
UPDATE festival SET age_restriction = 'ALL_AGES' WHERE age_restriction = 'NONE';
