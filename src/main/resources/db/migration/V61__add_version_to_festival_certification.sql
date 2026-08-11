-- 인증 승인/거절의 "PENDING 상태 확인 → 승인 처리" 사이 TOCTOU로 동시 승인 시
-- 포인트가 중복 지급될 수 있다. @Version 낙관적 락으로 방지한다 (V34와 동일 패턴).
ALTER TABLE `festival_certification` ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
