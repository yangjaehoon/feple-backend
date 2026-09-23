-- 멘션 대상 댓글이 익명이었는지 기록한다.
-- mentioned_user_id(= 대상 댓글의 작성자)만으로는 대상이 익명 댓글이었는지 알 수 없어,
-- 익명 댓글에 답글을 다는 것만으로 원 작성자의 실명 닉네임과 id가 응답에 노출됐다.
--
-- V1 baseline이 만드는 CI 테스트 DB와 운영 DB의 스키마가 다를 수 있어(baseline drift)
-- information_schema로 조건부 추가한다.

SET @col_exists = (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'comment'
      AND column_name = 'mentioned_anonymous');
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE `comment` ADD COLUMN `mentioned_anonymous` TINYINT(1) NOT NULL DEFAULT 0 AFTER `mentioned_user_id`',
    'DO 0');
PREPARE stmt FROM @add_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 행 백필: 대상 댓글을 특정할 수 없으므로(같은 글에 여러 번 댓글을 달 수 있음),
-- "멘션 대상 사용자가 그 글에 익명 댓글을 단 적이 있으면" 가린다.
-- 실명 댓글까지 함께 가려질 수 있지만, 노출보다 과하게 가리는 쪽이 안전한 방향이다.
UPDATE `comment` c
SET c.`mentioned_anonymous` = 1
WHERE c.`mentioned_user_id` IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM (SELECT * FROM `comment`) t
      WHERE t.`post_id` = c.`post_id`
        AND t.`user_id` = c.`mentioned_user_id`
        AND t.`anonymous` = 1
  );
