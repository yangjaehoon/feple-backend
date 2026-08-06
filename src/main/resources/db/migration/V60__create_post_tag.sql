-- 게시글 해시태그
CREATE TABLE IF NOT EXISTS `post_tag` (
    `id`      BIGINT      NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT      NOT NULL,
    `tag`     VARCHAR(30) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_post_tag_post_id_tag` (`post_id`, `tag`),
    KEY `idx_post_tag_tag` (`tag`),
    CONSTRAINT `fk_post_tag_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
