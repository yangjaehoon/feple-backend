-- V62에서 users.nickname UNIQUE 제약을 완전히 제거해 회원탈퇴(전원이 "(탈퇴한 사용자)"로
-- 익명화)는 고쳤지만, 부작용으로 활성 유저 사이의 닉네임 동시성 안전망까지 같이 사라졌다.
-- UserServiceImpl.updateNickname()은 existsByNicknameAndIdNot()으로 먼저 검사하고 나중에
-- flush하는 check-then-act 패턴인데, 동시 요청 경합을 막아주던 건 DB UNIQUE 제약 위반 시
-- DataIntegrityViolationException을 잡는 catch 블록이었다 — 제약이 없어지면서 이 catch가
-- 죽은 코드가 됐고, 두 유저가 동시에 같은 닉네임으로 바꾸면 둘 다 성공할 수 있다(닉네임
-- 사칭/커뮤니티 신뢰 문제). NicknameGenerator.uniquify()의 신규가입 닉네임 배정도 동일한 위험.
--
-- 생성 컬럼(virtual generated column)으로 "활성 유저일 때만 닉네임, 탈퇴 유저는 NULL"을
-- 만들어 그 컬럼에 UNIQUE를 걸면 MySQL이 NULL끼리는 유일성 검사를 안 해서(탈퇴 유저는
-- 전부 NULL로 서로 안 부딪힘) 원래 회원탈퇴 수정은 그대로 유지하면서, 활성 유저끼리의
-- 닉네임 유일성만 DB 레벨로 다시 원자적으로 강제한다.
ALTER TABLE users
    ADD COLUMN nickname_active_key VARCHAR(255)
    GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nickname ELSE NULL END) VIRTUAL;

ALTER TABLE users ADD UNIQUE INDEX uq_users_nickname_active (nickname_active_key);
