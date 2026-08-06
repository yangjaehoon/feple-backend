-- 게시글 다중 이미지 첨부 지원: post.image_url(단일) → post_image(다건) 이관

CREATE TABLE IF NOT EXISTS `post_image` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `post_id`    BIGINT       NOT NULL,
    `image_key`  VARCHAR(255) NOT NULL,
    `sort_order` INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_post_image_post_id` (`post_id`, `sort_order`),
    CONSTRAINT `fk_post_image_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `post_image` (`post_id`, `image_key`, `sort_order`)
SELECT `id`, `image_url`, 0 FROM `post` WHERE `image_url` IS NOT NULL AND `image_url` <> '';

ALTER TABLE `post` DROP COLUMN `image_url`;
