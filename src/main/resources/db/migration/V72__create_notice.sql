-- 관리자 공지사항. 앱 유저에게 노출되는 서비스 공지(점검/이벤트/정책 변경 안내)를
-- 배포 없이 등록·수정할 수 있도록 한다.
CREATE TABLE IF NOT EXISTS `notices` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(200) NOT NULL,
    `content`    TEXT        NOT NULL,
    `pinned`     BOOLEAN     NOT NULL DEFAULT FALSE,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notice_pinned_created` (`pinned`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
