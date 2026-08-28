-- 댓글 수정 여부를 명시적으로 기록. 예전엔 프론트가 updated_at - created_at 시간차(10초)로
-- 추정했다(blind/unblind로 updated_at이 바뀌어도 "수정됨"으로 오표시). 이 컬럼으로 대체하고,
-- 기존 행은 그 추정을 한 번 재현해 채워 배포 직후 사용자에게 보이는 값이 바뀌지 않게 한다.
ALTER TABLE `comment`
    ADD COLUMN `edited` TINYINT(1) NOT NULL DEFAULT 0 AFTER `blinded`;

UPDATE `comment`
SET `edited` = 1
WHERE TIMESTAMPDIFF(SECOND, `created_at`, `updated_at`) > 10;
