-- 탈퇴 유저는 전부 nickname="(탈퇴한 사용자)"로 익명화되는데, UNIQUE 제약 때문에
-- 두 번째 탈퇴 유저부터 회원탈퇴가 항상 실패했다. 활성 유저 사이의 닉네임 유일성은
-- UserRepository.existsByNickname 등이 deletedAt IS NULL로 이미 걸러서 검사하므로,
-- DB 레벨 UNIQUE는 제거하고 조회 성능용 일반 인덱스만 유지한다.
ALTER TABLE `users` DROP INDEX idx_user_nickname;
ALTER TABLE `users` ADD INDEX idx_user_nickname (nickname);
