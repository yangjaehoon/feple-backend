-- ── festival_certification.festival_id ──────────────────────────────────────
-- UNIQUE KEY (user_id, festival_id) 선두 컬럼이 user_id라 festival_id 단독 필터는 full scan
-- findByFestivalId / findApprovedUserIdsByFestivalId / existsApprovedCertification /
-- getAverageRatingByFestivalId / getRatingCountByFestivalId / getRatingDistributionByFestivalId /
-- findReviewsByFestivalId / deleteByFestivalId — 축제 상세 페이지 조회마다 호출되는 hot path
ALTER TABLE `festival_certification`
    ADD INDEX `idx_festival_certification_festival_id` (`festival_id`);
