-- notifications 테이블에 artist FK 추가
ALTER TABLE `notifications`
    ADD COLUMN `artist_id` BIGINT NULL AFTER `post_id`,
    ADD CONSTRAINT `fk_notif_artist` FOREIGN KEY (`artist_id`) REFERENCES `artist` (`id`) ON DELETE SET NULL;

-- artist_suggestion 테이블에 승인된 아티스트 ID 컬럼 추가
ALTER TABLE `artist_suggestion`
    ADD COLUMN `approved_artist_id` BIGINT NULL;
