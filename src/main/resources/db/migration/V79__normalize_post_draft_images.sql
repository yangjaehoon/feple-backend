-- post_draft.image_keys: 쉼표 구분 다중값 단일 컬럼 (1NF 위반) → post_draft_image 정규화 테이블
-- post.image_url → post_image(V54) 정규화와 동일하게 순서 보존 컬럼(sort_order)을 둔다.

CREATE TABLE `post_draft_image` (
    `user_id`    BIGINT       NOT NULL,
    `image_key`  VARCHAR(255) NOT NULL,
    `sort_order` INT          NOT NULL,
    PRIMARY KEY (`user_id`, `sort_order`),
    CONSTRAINT `fk_pdi_draft` FOREIGN KEY (`user_id`) REFERENCES `post_draft` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 CSV 값을 행 단위로 이관. V37 artist_aliases 정규화와 동일한 JSON_TABLE split 방식.
-- 빈 세그먼트는 버리고, sort_order는 유저별로 0부터 빈틈 없이 다시 매긴다
-- (@OrderColumn 리스트에 빈틈이 있으면 null 원소가 생겨 조회 시 NPE).
INSERT INTO `post_draft_image` (`user_id`, `image_key`, `sort_order`)
SELECT x.`user_id`,
       x.`image_key`,
       CAST(ROW_NUMBER() OVER (PARTITION BY x.`user_id` ORDER BY x.`seq`) AS SIGNED) - 1
FROM (
    SELECT d.`user_id` AS `user_id`,
           TRIM(jt.`k`) AS `image_key`,
           jt.`seq` AS `seq`
    FROM `post_draft` d,
         JSON_TABLE(
             CONCAT('["', REPLACE(REPLACE(TRIM(d.`image_keys`), '"', '\\"'), ',', '","'), '"]'),
             '$[*]' COLUMNS (`k` VARCHAR(255) PATH '$', `seq` FOR ORDINALITY)
         ) AS jt
    WHERE d.`image_keys` IS NOT NULL
      AND TRIM(d.`image_keys`) <> ''
      AND TRIM(jt.`k`) <> ''
) AS x;

ALTER TABLE `post_draft` DROP COLUMN `image_keys`;
