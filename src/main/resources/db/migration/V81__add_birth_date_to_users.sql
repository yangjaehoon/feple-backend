-- 나이 확인(age gate) 도입: 만 14세 미만 커뮤니티 이용 차단 (App Store 심사 5.1.1).
-- 기존 유저는 NULL로 남겨 다음 로그인 시 1회 생년월일 입력을 유도한다.
-- 미달 계정은 별도 컬럼 없이 기존 소프트 삭제(deleted_at + withdrawal_reason='AGE_RESTRICTED')를 재사용한다.
ALTER TABLE `users` ADD COLUMN `birth_date` DATE NULL AFTER `email`;
