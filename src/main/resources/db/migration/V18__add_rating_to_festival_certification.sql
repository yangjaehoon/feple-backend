-- 인증된 페스티벌에 대한 별점(1~5)·한줄 후기 컬럼 추가
ALTER TABLE festival_certification
    ADD COLUMN rating     TINYINT     NULL,
    ADD COLUMN user_review VARCHAR(100) NULL;
