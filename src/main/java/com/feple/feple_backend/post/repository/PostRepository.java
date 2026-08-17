package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ── 좋아요 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementLikeCount(@Param("id") Long id);

    // ── 스크랩 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.scrapCount = p.scrapCount + 1 WHERE p.id = :id")
    void incrementScrapCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET scrap_count = GREATEST(scrap_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementScrapCount(@Param("id") Long id);

    // ── 단건 조회 (user/artist/festival 연관 즉시 로딩) ─────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findWithAssociationsById(@Param("id") Long id);

    // ── 아티스트 게시글 커서 페이징 (id 기반 — 신규 게시글 삽입에 영향받지 않음) ──
    // 고정글은 fetchPinned 전용 쿼리로 첫 페이지에만 별도 노출하므로 일반 목록에서는 제외한다.
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist AND p.pinned = false ORDER BY p.id DESC")
    List<Post> findByArtistOrderByIdDesc(@Param("artist") Artist artist, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist AND p.pinned = false AND p.id < :cursor ORDER BY p.id DESC")
    List<Post> findByArtistAndIdLessThanOrderByIdDesc(@Param("artist") Artist artist, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist AND p.pinned = true ORDER BY p.createdAt DESC")
    List<Post> findByArtistAndPinnedTrueOrderByCreatedAtDesc(@Param("artist") Artist artist, Pageable pageable);

    // ── 페스티벌 일반 게시글 커서 페이징 (id 기반) ──────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL AND p.pinned = false ORDER BY p.id DESC")
    List<Post> findGeneralFestivalPostsOrderByIdDesc(@Param("festival") Festival festival, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL AND p.pinned = false AND p.id < :cursor ORDER BY p.id DESC")
    List<Post> findGeneralFestivalPostsAndIdLessThanOrderByIdDesc(@Param("festival") Festival festival, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL AND p.pinned = true ORDER BY p.createdAt DESC")
    List<Post> findGeneralFestivalPinnedPostsOrderByCreatedAtDesc(@Param("festival") Festival festival, Pageable pageable);

    // ── 페스티벌+게시판타입 커서 페이징 (id 기반) ────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType AND p.pinned = false ORDER BY p.id DESC")
    List<Post> findByFestivalAndBoardTypeOrderByIdDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType AND p.pinned = false AND p.id < :cursor ORDER BY p.id DESC")
    List<Post> findByFestivalAndBoardTypeAndIdLessThanOrderByIdDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType AND p.pinned = true ORDER BY p.createdAt DESC")
    List<Post> findByFestivalAndBoardTypeAndPinnedTrueOrderByCreatedAtDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findByFestivalOrderByLikeCountDesc(@Param("festival") Festival festival, Pageable pageable);

    // 마이페이지 표시용 — 최신순 정렬, 상한선 적용 (Pageable)
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // ── 내 게시글 커서 페이징 (id 기반) ──────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByUserOrderByIdDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByUserAndIdLessThanOrderByIdDesc(User user, Long id, Pageable pageable);

    // 타인 프로필 표시용 — 익명 게시글 제외
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user AND p.anonymous = false ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user = :user AND p.anonymous = false")
    Page<Post> findPublicByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    // ── 공개(타인 프로필) 게시글 커서 페이징 (id 기반) ────────────────────────
    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user AND p.anonymous = false ORDER BY p.id DESC")
    List<Post> findPublicByUserOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user AND p.anonymous = false AND p.id < :cursor ORDER BY p.id DESC")
    List<Post> findPublicByUserAndIdLessThanOrderByIdDesc(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);


    // 관리자 상세 — userId 직접 사용 (User 엔티티 사전 조회 불필요)
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    Page<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.anonymous = false")
    long countPublicByUserId(@Param("userId") Long userId);

    // ── 인기 게시글 (N+1: user/artist/festival 접근) ───────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC")
    List<Post> findPopularPosts(@Param("since") LocalDateTime since, Pageable pageable);

    // ── 게시판 타입별 (N+1: user/artist/festival 접근) ───────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    // ── 커서 기반 최신순 (id < cursor) ─────────────────────────────────────────
    // 고정글은 fetchPinned 전용 쿼리로 첫 페이지에만 별도 노출하므로 일반 목록에서는 제외한다.
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByBoardTypeAndPinnedFalseOrderByIdDesc(BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByBoardTypeAndPinnedFalseAndIdLessThanOrderByIdDesc(BoardType boardType, Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByBoardTypeAndPinnedTrueOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    // id를 최종 타이브레이커로 둬 likeCount가 동률이거나 페이지 조회 사이에 변경돼도
    // 페이지 경계에서 게시글이 중복/누락되지 않도록 한다
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByBoardTypeOrderByLikeCountDescCreatedAtDescIdDesc(BoardType boardType, Pageable pageable);

    // ── 검색 (사용자 통합 검색 전용 — FULLTEXT ngram, 대량 데이터에서도 인덱스 사용) ──
    // LIKE '%kw%'는 B-tree 인덱스를 못 타 풀스캔이었음 — FULLTEXT 매치로 id만 먼저
    // 뽑은 뒤 EntityGraph로 연관 엔티티를 한 번에 가져와 N+1을 피한다.
    // REPLACE로 큰따옴표를 제거해 boolean 모드 phrase 구문이 깨지지 않게 방어한다.
    // 관리자 필터 검색(findByTitleContainingIgnoreCaseOrderByCreatedAtDesc 등)은
    // JpqlLikeEscaper로 이스케이프한 값을 그대로 쓰므로 이 메서드들과 공유하지 않는다.
    @Query(value = "SELECT id FROM post WHERE MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE) ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM post WHERE MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE)",
           nativeQuery = true)
    Page<Long> searchTitleIds(@Param("kw") String kw, Pageable pageable);

    @Query(value = "SELECT id FROM post WHERE board_type = :boardType AND MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE) ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM post WHERE board_type = :boardType AND MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE)",
           nativeQuery = true)
    Page<Long> searchTitleIdsByBoardType(@Param("boardType") String boardType, @Param("kw") String kw, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.id IN :ids")
    List<Post> findAllWithAssociationsByIdIn(@Param("ids") List<Long> ids);

    default Page<Post> searchPostsByTitleFullText(String kw, Pageable pageable) {
        return reorderByFullTextMatch(searchTitleIds(kw, stripSort(pageable)), pageable);
    }

    default Page<Post> searchPostsByBoardTypeAndTitleFullText(BoardType boardType, String kw, Pageable pageable) {
        return reorderByFullTextMatch(searchTitleIdsByBoardType(boardType.name(), kw, stripSort(pageable)), pageable);
    }

    // native 쿼리(searchTitleIds/searchTitleIdsByBoardType)는 정렬이 이미 SQL의
    // ORDER BY created_at DESC로 고정돼 있다. Pageable에 property 기반 Sort가 실려
    // 그대로 전달되면 Spring이 이를 컬럼명으로 오인해 붙이면서 "Unknown column" SQL
    // 에러가 날 수 있다(관리자 아티스트 목록 findByGenre에서 실제로 발생했던 버그와 동일
    // 패턴) — 페이지 번호/크기만 남기고 Sort는 native 쿼리에 도달하기 전에 제거한다.
    private static Pageable stripSort(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private Page<Post> reorderByFullTextMatch(Page<Long> idsPage, Pageable pageable) {
        Map<Long, Post> byId = findAllWithAssociationsByIdIn(idsPage.getContent()).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> ordered = idsPage.getContent().stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, idsPage.getTotalElements());
    }

    // ── 검색 (관리자 필터) ───────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!' ORDER BY p.createdAt DESC")
    Page<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(@Param("kw") String kw, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!' ORDER BY p.createdAt DESC")
    Page<Post> findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(@Param("boardType") BoardType boardType, @Param("kw") String kw, Pageable pageable);

    // 아티스트 게시판 전체
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist IS NOT NULL ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist IS NOT NULL AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!' ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIsNotNullAndTitleLikeOrderByCreatedAtDesc(@Param("kw") String keyword, Pageable pageable);

    // 특정 아티스트 게시판
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist.id = :artistId ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIdOrderByCreatedAtDesc(@Param("artistId") Long artistId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist.id = :artistId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!' ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIdAndTitleLikeOrderByCreatedAtDesc(@Param("artistId") Long artistId, @Param("kw") String keyword, Pageable pageable);

    // 페스티벌 게시판 전체
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival IS NOT NULL ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival IS NOT NULL AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!' ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIsNotNullAndTitleLikeOrderByCreatedAtDesc(@Param("kw") String keyword, Pageable pageable);

    // 특정 페스티벌 게시판
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival.id = :festivalId ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIdOrderByCreatedAtDesc(@Param("festivalId") Long festivalId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival.id = :festivalId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!' ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIdAndTitleLikeOrderByCreatedAtDesc(@Param("festivalId") Long festivalId, @Param("kw") String keyword, Pageable pageable);

    // ── 관리자 배치 카운트 ────────────────────────────────────────────────────
    @Query("SELECT p.user.id, COUNT(p) FROM Post p WHERE p.user.id IN :userIds GROUP BY p.user.id")
    List<Object[]> countGroupByUserId(@Param("userIds") List<Long> userIds);

    // ── 금칙어 스캔 ───────────────────────────────────────────────────────────
    @Query("SELECT COUNT(p) FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :word, '%')) ESCAPE '!' OR LOWER(p.content) LIKE LOWER(CONCAT('%', :word, '%')) ESCAPE '!'")
    long countByTitleOrContentContaining(@Param("word") String word);

    // ── 통계 ─────────────────────────────────────────────────────────────────
    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt > :since AND p.deletedAt IS NULL")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :start AND p.createdAt < :end AND p.deletedAt IS NULL")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', p.createdAt), COUNT(p) FROM Post p " +
           "WHERE p.createdAt >= :from AND p.createdAt < :to GROUP BY FUNCTION('DATE', p.createdAt)")
    List<Object[]> countGroupByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.artist.id = :artistId AND p.createdAt >= :since")
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    /** 벌크 랭킹용: [artistId, postCount, likeSum] */
    @Query("SELECT p.artist.id, COUNT(p), COALESCE(SUM(p.likeCount), 0) " +
           "FROM Post p WHERE p.artist IS NOT NULL AND p.createdAt >= :since " +
           "GROUP BY p.artist.id")
    List<Object[]> countAndSumByArtistSince(@Param("since") LocalDateTime since);

    // ── 댓글 카운트 (원자적 증감 — race condition 방지) ───────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    void decrementCommentCount(@Param("postId") Long postId);

    // ── 조회수 카운트 ─────────────────────────────────────────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    // ── Soft delete 관리자용 ──────────────────────────────────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET deleted_at = NOW() WHERE id IN (:ids)", nativeQuery = true)
    void softDeleteByIds(@Param("ids") List<Long> ids);

    // @SQLRestriction을 우회하는 네이티브 쿼리 — 관리자 휴지통 조회용
    @Query(value = "SELECT * FROM post WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findSoftDeleted(@Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restore(@Param("id") Long id);

    // ── 블라인드 관리자용 ──────────────────────────────────────────────────────
    // @SQLRestriction을 우회하는 네이티브 쿼리 — 블라인드된 글도 관리자는 검토할 수 있어야 함
    @Query(value = "SELECT * FROM post WHERE id = :id", nativeQuery = true)
    Optional<Post> findByIdIgnoringRestrictions(@Param("id") Long id);

    // findByIdIgnoringRestrictions의 배치 버전 — 신고 일괄 반려처럼 여러 건의 블라인드 해제 대상을
    // 한 번에 조회해야 할 때 id마다 반복 호출하지 않도록 한다.
    @Query(value = "SELECT * FROM post WHERE id IN (:ids)", nativeQuery = true)
    List<Post> findAllByIdInIgnoringRestrictions(@Param("ids") Collection<Long> ids);

    // 블라인드된 글은 일반 목록/검색(@SQLRestriction)에서 전혀 노출되지 않아 관리자가 찾을 방법이
    // 없으므로, 삭제된 글의 휴지통(findSoftDeleted)과 동일한 패턴으로 전용 목록을 제공한다.
    @Query(value = "SELECT * FROM post WHERE blinded = true AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findBlinded(@Param("limit") int limit);

}
