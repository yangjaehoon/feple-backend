-- FestivalCertification에 리뷰 추천 수 컬럼 추가
ALTER TABLE festival_certification
    ADD COLUMN like_count INT NOT NULL DEFAULT 0;

-- 리뷰 추천 테이블 생성 (사용자당 리뷰 하나에 1회 추천)
CREATE TABLE review_like (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    certification_id BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_like_user_cert (user_id, certification_id),
    KEY idx_review_like_user (user_id),
    KEY idx_review_like_cert (certification_id)
);
