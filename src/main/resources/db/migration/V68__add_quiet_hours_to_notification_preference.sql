-- 심야시간(00:00~07:00 KST) 푸시 알림 차단 온오프 설정. 관리자 수동 발송(AdminPushService)은
-- NotificationPreference를 거치지 않고 FCM을 직접 호출하므로 이 설정의 영향을 받지 않는다.
ALTER TABLE `notification_preferences` ADD COLUMN `quiet_hours_enabled` BOOLEAN NOT NULL DEFAULT FALSE;
