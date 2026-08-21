-- ============================================================
-- festival_ticket_link (depends on festival)
-- ============================================================
CREATE TABLE IF NOT EXISTS `festival_ticket_link` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `festival_id` BIGINT       NOT NULL,
    `label`       VARCHAR(100) NULL,
    `url`         VARCHAR(500) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_festival_ticket_link_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
