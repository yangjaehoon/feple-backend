ALTER TABLE `users`
    ADD COLUMN `withdrawal_reason` VARCHAR(30)  NULL AFTER `deleted_at`,
    ADD COLUMN `withdrawal_detail` VARCHAR(300) NULL AFTER `withdrawal_reason`;
