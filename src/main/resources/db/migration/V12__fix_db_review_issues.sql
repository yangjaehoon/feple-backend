-- ── 1. comment.parent_id FK: ON DELETE SET NULL ─────────────────────────────
-- FK 이름이 환경마다 다를 수 있으므로 information_schema에서 실제 이름을 조회해 삭제

SET @comment_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA  = DATABASE()
      AND TABLE_NAME    = 'comment'
      AND COLUMN_NAME   = 'parent_id'
      AND REFERENCED_TABLE_NAME = 'comment'
    LIMIT 1
);

SET @drop_comment_fk = IF(
    @comment_fk IS NOT NULL,
    CONCAT('ALTER TABLE `comment` DROP FOREIGN KEY `', @comment_fk, '`'),
    'DO 0'
);
PREPARE s FROM @drop_comment_fk; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE `comment`
    ADD CONSTRAINT `fk_comment_parent`
        FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`)
        ON DELETE SET NULL;

-- ── 2. festival_checklist → festival FK (ON DELETE CASCADE) ─────────────────
-- festival_checklist 에 festival FK 가 없어 페스티벌 삭제 시 체크리스트 고아 행 발생

SET @fc_fk_count = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA  = DATABASE()
      AND TABLE_NAME    = 'festival_checklist'
      AND REFERENCED_TABLE_NAME = 'festival'
);

SET @add_fc_fk = IF(
    @fc_fk_count = 0,
    'ALTER TABLE `festival_checklist` ADD CONSTRAINT `fk_fc_festival` FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`) ON DELETE CASCADE',
    'DO 0'
);
PREPARE s FROM @add_fc_fk; EXECUTE s; DEALLOCATE PREPARE s;

-- ── 3. users.email 인덱스 추가 ───────────────────────────────────────────────
-- findByEmail 쿼리 full scan 방지

SET @email_idx_count = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'users'
      AND INDEX_NAME   = 'idx_users_email'
);

SET @add_email_idx = IF(
    @email_idx_count = 0,
    'ALTER TABLE `users` ADD INDEX `idx_users_email` (`email`)',
    'DO 0'
);
PREPARE s FROM @add_email_idx; EXECUTE s; DEALLOCATE PREPARE s;

-- ── 4. refresh_tokens 중복 인덱스 제거 ──────────────────────────────────────
-- uq_refresh_token_hash (UNIQUE) 가 이미 인덱스를 생성하므로 완전 중복

SET @dup_idx_count = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'refresh_tokens'
      AND INDEX_NAME   = 'idx_refresh_token_hash'
);

SET @drop_dup_idx = IF(
    @dup_idx_count > 0,
    'ALTER TABLE `refresh_tokens` DROP INDEX `idx_refresh_token_hash`',
    'DO 0'
);
PREPARE s FROM @drop_dup_idx; EXECUTE s; DEALLOCATE PREPARE s;
