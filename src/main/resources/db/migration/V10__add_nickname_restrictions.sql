CREATE TABLE `nickname_restrictions` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `word`       VARCHAR(50) NOT NULL,
    `created_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_nickname_restrictions_word` (`word`)
);
