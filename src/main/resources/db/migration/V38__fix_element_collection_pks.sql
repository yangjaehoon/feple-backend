-- festival_genres, admin_account_permissions: PRIMARY KEY 없어 중복 행 삽입 가능 (1NF 위반)
-- @ElementCollection Set 이므로 애플리케이션 레벨은 중복이 없지만 DB 제약이 없었음

-- festival_genres: (festival_id, genres) 복합 UNIQUE 추가
ALTER TABLE festival_genres
    ADD UNIQUE KEY uq_fg_festival_genre (festival_id, genres);

-- admin_account_permissions: NULL permission 행 제거 후 NOT NULL + UNIQUE 추가
DELETE FROM admin_account_permissions WHERE permission IS NULL;
ALTER TABLE admin_account_permissions
    MODIFY COLUMN permission VARCHAR(30) NOT NULL;
ALTER TABLE admin_account_permissions
    ADD UNIQUE KEY uq_aap_account_permission (admin_account_id, permission);
