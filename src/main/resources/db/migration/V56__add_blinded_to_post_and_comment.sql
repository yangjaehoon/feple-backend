-- 신고 누적 시 자동 블라인드 지원
ALTER TABLE `post`
    ADD COLUMN `blinded` TINYINT(1) NOT NULL DEFAULT 0 AFTER `pinned`;

ALTER TABLE `comment`
    ADD COLUMN `blinded` TINYINT(1) NOT NULL DEFAULT 0 AFTER `anonymous`;
