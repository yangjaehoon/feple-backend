DROP INDEX IF EXISTS idx_user_nickname ON `user`;
ALTER TABLE `user` ADD UNIQUE INDEX idx_user_nickname (nickname);
