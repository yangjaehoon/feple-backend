INSERT INTO festival (id, title, location, start_date, end_date, description, poster_url)
VALUES (1, '썸데이페스티벌 2025', '서울 난지한강공원', '2025-05-20', '2025-05-21', '감성 충만한 서울 대표 페스티벌',
        'https://example.com/posters/someday.jpg'),
       (2, '그린플러그드 서울 2025', '서울 난지한강공원', '2025-05-04', '2025-05-05', '환경을 생각하는 음악 축제',
        'https://example.com/posters/greenplugged.jpg'),
       (3, '뷰티풀 민트 라이프 2025', '올림픽공원 88잔디마당', '2025-05-11', '2025-05-12', '잔잔한 인디 음악 페스티벌',
        'https://example.com/posters/bml.jpg'),
       (4, '자라섬 재즈 페스티벌 2025', '가평 자라섬', '2025-10-11', '2025-10-13', '재즈의 향연, 깊은 가을에 열리는 축제',
        'https://example.com/posters/jarasum.jpg'),
       (5, '울트라 코리아 2025', '서울 잠실 종합운동장', '2025-06-08', '2025-06-09', 'EDM과 열정의 끝판왕',
        'https://example.com/posters/ultrakorea.jpg');


INSERT INTO artist (name, genre, like_count, profile_image_url, festival_id)
VALUES ('BewhY', 'Hip-hop', 0, 'https://example.com/images/bewhy.jpg', 1),
       ('ZICO', 'Hip-hop', 0, 'https://example.com/images/zico.jpg', 2),
       ('Jay Park', 'Hip-hop', 0, 'https://example.com/images/jaypark.jpg', 2),
       ('Changmo', 'Hip-hop', 0, 'https://example.com/images/changmo.jpg', 3),
       ('Loco', 'Hip-hop', 0, 'https://example.com/images/loco.jpg', 3),
       ('Simon Dominic', 'Hip-hop', 0, 'https://example.com/images/simon.jpg', 4),
       ('pH-1', 'Hip-hop', 0, 'https://example.com/images/ph1.jpg', 4),
       ('Ash Island', 'Hip-hop', 0, 'https://example.com/images/ashisland.jpg', 5),
       ('Coogie', 'Hip-hop', 0, 'https://example.com/images/coogie.jpg', 5),
       ('Leellamarz', 'Hip-hop', 0, 'https://example.com/images/leellamarz.jpg', 1);

INSERT INTO users (id, email, nickname, oauth_id, provider, profile_image_url)
VALUES (1, 'test@example.com', '테스트유저', 'oauth_dummy_id', 'GOOGLE', 'https://example.com/image.jpg');


-- 자유 게시판 더미 데이터
INSERT INTO post (title, content, like_count, created_at, updated_at, user_id, board_type)
VALUES ('첫 글입니다!', '안녕하세요, FEPLE 자유 게시판에 오신 것을 환영합니다!', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('페스티벌 추천해주세요!', '이번 여름에 갈만한 페스티벌이 있을까요?', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('좋아하는 아티스트 공유해요', '저는 빈지노 팬인데, 여러분은 누구 좋아하세요?', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('페스티벌 꿀팁!', '물 많이 마시고, 편한 신발 꼭 신으세요.', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('친구들이랑 가는게 좋을까요?', '혼자 갈까 고민 중인데 조언 부탁해요.', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('비 올 때 대비 팁', '방수포랑 우비는 필수!', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('입장 시간 질문이요', '보통 몇 시에 가야 잘 볼 수 있을까요?', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('먹거리 추천 부탁!', '페스티벌 현장에서 맛있는 음식 추천해 주세요.', 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('의외로 조용한 부스 추천', '사람 적은 쉴 곳 있으면 알려주세요.', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE'),
       ('캠핑 자리 꿀팁', '좋은 위치 선점하려면 언제부터 가야 하나요?', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'FREE');

-- 동행 게시판 더미 데이터
INSERT INTO post (title, content, board_type, like_count, created_at, updated_at, user_id)
VALUES
    ('같이 서울재즈페스티벌 가실 분?', '혼자 가기 좀 그래서 동행 구해요! 티켓 있어요.', 'MATE', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('2025 부산 락페 동행해요~', '락페 처음인데 같이 가실 분 환영합니다!', 'MATE', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    ('전주 벚꽃 축제 동행 구해요', '혼자는 심심해서요 ㅠㅠ 같이 산책하실 분!', 'MATE', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('서울숲 재즈 거리축제', '일정 맞는 분 같이 다녀오면 좋을 것 같아요', 'MATE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3),
    ('서울 EDM 페스티벌 동행', '혼자보다 같이 즐기면 좋잖아요?', 'MATE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2);
