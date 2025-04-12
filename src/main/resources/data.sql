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