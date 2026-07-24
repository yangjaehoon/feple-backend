ALTER TABLE user_device_tokens
    ADD COLUMN language VARCHAR(10) NOT NULL DEFAULT 'ko';
