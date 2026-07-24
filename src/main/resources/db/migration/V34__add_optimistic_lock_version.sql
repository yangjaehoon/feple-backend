-- 관리자 페이지의 "폼 로드 → 수정 → 저장" 패턴은 두 관리자가 동시에 같은
-- 페스티벌/아티스트/관리자계정을 편집하면 나중에 저장한 쪽이 먼저 저장된
-- 내용을 조용히 덮어쓴다(lost update). @Version 낙관적 락으로 방지한다.
ALTER TABLE `festival`       ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `artist`         ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `admin_accounts` ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0;
