-- UserRepository.findByEmail() 삭제(미사용 확인)에 따라 해당 조회 전용으로
-- V12에서 추가했던 인덱스도 함께 정리한다 — 더 이상 어떤 쿼리도 이 인덱스를
-- 활용하지 않으므로 쓰기 시 유지 비용만 남는다.
ALTER TABLE `users` DROP INDEX `idx_users_email`;
