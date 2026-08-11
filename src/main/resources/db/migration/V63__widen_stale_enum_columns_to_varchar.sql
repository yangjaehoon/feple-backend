-- post_report/comment_report/artist_photo_report.status, artist_suggestion.status, post.board_type는
-- DB에 네이티브 MySQL ENUM으로 만들어져 있었는데, Java enum(ReportStatus/ArtistSuggestionStatus/BoardType)이
-- 이후 값을 늘렸을 때 DB ENUM 정의는 갱신되지 않았다(ddl-auto=none이라 Hibernate가 감지 못함).
-- 그 결과 strict SQL mode에서 다음 흐름이 전부 SQL 예외로 실패하고 있었다:
--   - ReportRejectionService.reject/bulkReject → status='REJECTED' (ENUM에 REJECTED 없음, DISMISSED만 있음)
--   - ArtistSuggestion.approve() → status='APPROVED' (ENUM에 APPROVED 없음)
--   - PostController의 페스티벌 동행/티켓 게시판 글쓰기 → board_type='FESTIVAL_COMPANION'/'FESTIVAL_TICKET' (ENUM에 둘 다 없음)
-- users.role/users.provider/festival_suggestion.status처럼 VARCHAR로 통일해 이 문제가 재발하지 않게 한다.
ALTER TABLE post_report MODIFY COLUMN status VARCHAR(20) NOT NULL;
ALTER TABLE comment_report MODIFY COLUMN status VARCHAR(20) NOT NULL;
ALTER TABLE artist_photo_report MODIFY COLUMN status VARCHAR(20) NOT NULL;
ALTER TABLE artist_suggestion MODIFY COLUMN status VARCHAR(20) NOT NULL;
ALTER TABLE post MODIFY COLUMN board_type VARCHAR(30) NULL;
