-- V43에서 누락됐던 s3_key 컬럼을 DEFAULT ''로 채워 넣은 결과, 컬럼이 없던
-- 시절 생성된 artist_photos 로우는 s3_key=''가 됨. 이 로우들은 애초에 S3
-- 오브젝트가 저장된 적이 없는 깨진 데이터라 presigned URL 생성 시
-- "Key cannot be empty" 오류로 조회 자체가 실패한다. 렌더링 불가능한
-- 데이터이므로 FK 의존 순서(좋아요 → 신고 → 사진)대로 정리한다.
DELETE FROM `artist_photo_likes`
WHERE `artist_photo_id` IN (SELECT `id` FROM `artist_photos` WHERE `s3_key` = '');

DELETE FROM `artist_photo_report`
WHERE `photo_id` IN (SELECT `id` FROM `artist_photos` WHERE `s3_key` = '');

DELETE FROM `artist_photos` WHERE `s3_key` = '';
