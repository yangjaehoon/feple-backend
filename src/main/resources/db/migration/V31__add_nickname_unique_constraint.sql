-- 중복 닉네임이 있으면 뒤에 _id를 붙여 유니크하게 만든 뒤 인덱스 추가
UPDATE `user` u1
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY nickname ORDER BY id) AS rn
    FROM `user`
    WHERE nickname IS NOT NULL
) ranked ON u1.id = ranked.id
SET u1.nickname = CONCAT(u1.nickname, '_', u1.id)
WHERE ranked.rn > 1;

ALTER TABLE `user` ADD UNIQUE INDEX idx_user_nickname (nickname);
