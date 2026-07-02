-- festival_genres.genres 컬럼이 MySQL ENUM 타입으로 생성된 경우
-- 새로 추가된 Genre 값(ETC 등)을 수용하지 못해 Data truncated 에러 발생.
-- VARCHAR(20)으로 명시적 변경하여 Java enum name 저장을 보장한다.
ALTER TABLE festival_genres
    MODIFY COLUMN genres VARCHAR(20) NOT NULL;
