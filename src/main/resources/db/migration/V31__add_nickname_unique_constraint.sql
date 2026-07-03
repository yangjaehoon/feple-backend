DROP INDEX IF EXISTS idx_user_nickname ON `users`;
ALTER TABLE `users` ADD UNIQUE INDEX idx_user_nickname (nickname);
