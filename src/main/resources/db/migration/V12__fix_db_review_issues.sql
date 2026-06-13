-- ── 1. comment.parent_id FK: ON DELETE SET NULL ─────────────────────────────
-- 부모 댓글이 삭제될 때 자식 댓글도 함께 삭제되지 않고 익명 처리되도록 변경.
-- 기존 RESTRICT 제약은 댓글 일괄 삭제(deleteByPostIds) 시 FK 위반 발생 위험.
ALTER TABLE `comment`
    DROP FOREIGN KEY `fk_comment_parent`,
    ADD CONSTRAINT `fk_comment_parent`
        FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`)
        ON DELETE SET NULL;

-- ── 2. festival_checklist → festival FK (ON DELETE CASCADE) ─────────────────
-- festival_checklist 에 festival FK 가 없어 페스티벌 삭제 시 체크리스트가 고아 행으로 남음.
ALTER TABLE `festival_checklist`
    ADD CONSTRAINT `fk_fc_festival`
        FOREIGN KEY (`festival_id`) REFERENCES `festival` (`id`)
        ON DELETE CASCADE;

-- ── 3. users.email 인덱스 추가 ───────────────────────────────────────────────
-- findByEmail 쿼리가 full scan 발생 (이메일 로그인·OAuth 매핑에서 빈번히 사용됨)
ALTER TABLE `users`
    ADD INDEX `idx_users_email` (`email`);

-- ── 4. refresh_tokens 중복 인덱스 제거 ──────────────────────────────────────
-- uq_refresh_token_hash (UNIQUE) 가 이미 인덱스를 생성하므로
-- idx_refresh_token_hash 는 완전 중복 — 삭제하여 쓰기 오버헤드 제거
ALTER TABLE `refresh_tokens`
    DROP INDEX `idx_refresh_token_hash`;
