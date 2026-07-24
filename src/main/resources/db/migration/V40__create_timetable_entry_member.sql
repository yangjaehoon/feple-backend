CREATE TABLE IF NOT EXISTS `timetable_entry_member` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `entry_id`    BIGINT       NOT NULL,
    `artist_id`   BIGINT       NULL,
    `artist_name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_tem_entry`  FOREIGN KEY (`entry_id`)  REFERENCES `timetable_entry` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tem_artist` FOREIGN KEY (`artist_id`) REFERENCES `artist` (`id`) ON DELETE SET NULL,
    INDEX `idx_timetable_entry_member_entry_id` (`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
