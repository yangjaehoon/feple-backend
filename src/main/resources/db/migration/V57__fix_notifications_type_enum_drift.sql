-- notifications.type이 마이그레이션 이력에 없는 MySQL ENUM으로 운영 DB에서 드리프트돼
-- 있었음(Flyway 밖에서 수동 변경된 것으로 추정, V1 baseline은 VARCHAR(30)). ENUM 목록에
-- ADMIN_POINT_GRANTED가 빠져 있어 포인트 지급 알림 저장이 매번 "Data truncated" 에러로
-- 실패하던 원인. VARCHAR(30)으로 되돌려 앞으로 NotificationType이 추가될 때마다 DB ENUM을
-- 같이 바꿔야 하는 드리프트 재발 가능성 자체를 제거한다.
ALTER TABLE `notifications`
    MODIFY COLUMN `type` VARCHAR(30) NOT NULL;
