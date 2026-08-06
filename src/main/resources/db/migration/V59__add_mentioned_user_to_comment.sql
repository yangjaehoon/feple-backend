-- 댓글 멘션: 답글이 실제로 어느 댓글(작성자)을 향한 것인지 별도로 기록한다.
-- depth 1단계로 평탄화되면서 parent_id만으로는 이 정보가 사라지기 때문.
ALTER TABLE `comment`
    ADD COLUMN `mentioned_user_id` BIGINT NULL AFTER `parent_id`,
    ADD CONSTRAINT `fk_comment_mentioned_user` FOREIGN KEY (`mentioned_user_id`) REFERENCES `users` (`id`);
