-- ArtistProfileImage/ArtistProfileImageLike 기능 제거.
-- 애플리케이션 코드 어디에도 artist_image에 행을 저장(save)하는 경로가 없어
-- 두 테이블 모두 처음부터 데이터가 쌓일 수 없었던 죽은 기능이었음.
-- FK 참조 순서상 자식 테이블(artist_image_like)을 먼저 제거한다.
DROP TABLE IF EXISTS `artist_image_like`;
DROP TABLE IF EXISTS `artist_image`;
