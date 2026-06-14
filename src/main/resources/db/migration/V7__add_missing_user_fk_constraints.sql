-- notification_preferences, artist_suggestion, song_request 테이블에
-- user_id FK가 누락되어 있어 DB 레벨 참조 정합성 보증이 없었음.
-- UserCascadeDeleteService가 앱 레벨에서 순서 보장하므로 ON DELETE RESTRICT 사용.

-- 혹시 존재하는 고아 행 정리 (정상 운영 환경에서는 없어야 함)
DELETE FROM `notification_preferences` WHERE `user_id` NOT IN (SELECT `id` FROM `users`);
DELETE FROM `artist_suggestion`        WHERE `user_id` NOT IN (SELECT `id` FROM `users`);
DELETE FROM `song_request`             WHERE `user_id` NOT IN (SELECT `id` FROM `users`);

ALTER TABLE `notification_preferences`
    ADD CONSTRAINT `fk_np_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `artist_suggestion`
    ADD CONSTRAINT `fk_as_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `song_request`
    ADD CONSTRAINT `fk_sr_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
