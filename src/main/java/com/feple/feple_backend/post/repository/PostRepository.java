package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import java.time.LocalDateTime;
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

/**
 * 공개 조회 메서드는 삭제(deleted_at)·블라인드(blinded) 행을 명시적으로 제외한다
 * (Festival/Artist와 동일 방식 — 상시 @SQLRestriction 미사용).
 * 관리자·본인·신고·캐스케이드 경로는 기본 {@code findById}/{@code deleteById}로 모든 행을 다룬다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    String VISIBLE = " AND p.deletedAt IS NULL AND p.blinded = false";

    // ── 좋아요 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE post SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementLikeCount(@Param("id") Long id);

    // ── 스크랩 카운터 ────────────────────────────────────────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.scrapCount = p.scrapCount + 1 WHERE p.id = :id")
    void incrementScrapCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE post SET scrap_count = GREATEST(scrap_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementScrapCount(@Param("id") Long id);

    // ── 공개 단건 조회 ──────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.id = :id" + VISIBLE)
    Optional<Post> findWithAssociationsById(@Param("id") Long id);

    // ── 아티스트 게시글 커서 페이징 (id 기반) ───────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist AND p.pinned = false" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByArtistOrderByIdDesc(@Param("artist") Artist artist, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist AND p.pinned = false AND p.id < :cursor" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByArtistAndIdLessThanOrderByIdDesc(@Param("artist") Artist artist, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist AND p.pinned = true" + VISIBLE + " ORDER BY p.createdAt DESC")
    List<Post> findByArtistAndPinnedTrueOrderByCreatedAtDesc(@Param("artist") Artist artist, Pageable pageable);

    // ── 페스티벌 일반 게시글 커서 페이징 ────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL AND p.pinned = false" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findGeneralFestivalPostsOrderByIdDesc(@Param("festival") Festival festival, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL AND p.pinned = false AND p.id < :cursor" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findGeneralFestivalPostsAndIdLessThanOrderByIdDesc(@Param("festival") Festival festival, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL AND p.pinned = true" + VISIBLE + " ORDER BY p.createdAt DESC")
    List<Post> findGeneralFestivalPinnedPostsOrderByCreatedAtDesc(@Param("festival") Festival festival, Pageable pageable);

    // ── 페스티벌+게시판타입 커서 페이징 ─────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType AND p.pinned = false" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByFestivalAndBoardTypeOrderByIdDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType AND p.pinned = false AND p.id < :cursor" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByFestivalAndBoardTypeAndIdLessThanOrderByIdDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType AND p.pinned = true" + VISIBLE + " ORDER BY p.createdAt DESC")
    List<Post> findByFestivalAndBoardTypeAndPinnedTrueOrderByCreatedAtDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival" + VISIBLE + " ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findByFestivalOrderByLikeCountDesc(@Param("festival") Festival festival, Pageable pageable);

    // ── 내 게시글 (마이페이지 — 삭제·블라인드 제외) ──────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE p.user = :user" + VISIBLE + " ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user = :user" + VISIBLE)
    Page<Post> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.user = :user" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByUserOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.id < :cursor" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByUserAndIdLessThanOrderByIdDesc(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    // 타인 프로필 표시용 — 익명 게시글 + 삭제·블라인드 제외
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user AND p.anonymous = false" + VISIBLE + " ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user = :user AND p.anonymous = false" + VISIBLE)
    Page<Post> findPublicByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user AND p.anonymous = false" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findPublicByUserOrderByIdDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user AND p.anonymous = false AND p.id < :cursor" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findPublicByUserAndIdLessThanOrderByIdDesc(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    // 관리자 상세 — userId 직접 사용 (삭제·블라인드 제외 — 현재 가시성 유지)
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE p.user.id = :userId" + VISIBLE + " ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId" + VISIBLE)
    Page<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId" + VISIBLE)
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.anonymous = false" + VISIBLE)
    long countPublicByUserId(@Param("userId") Long userId);

    // ── 인기 게시글 ─────────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since" + VISIBLE + " ORDER BY p.likeCount DESC")
    List<Post> findPopularPosts(@Param("since") LocalDateTime since, Pageable pageable);

    // ── 게시판 타입별 목록 ──────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE 1 = 1" + VISIBLE + " ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE 1 = 1" + VISIBLE)
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE p.boardType = :boardType" + VISIBLE + " ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.boardType = :boardType" + VISIBLE)
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(@Param("boardType") BoardType boardType, Pageable pageable);

    // ── 커서 기반 최신순 (id < cursor) — 고정글은 fetchPinned 전용으로 분리 ──
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND p.pinned = false" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByBoardTypeAndPinnedFalseOrderByIdDesc(@Param("boardType") BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND p.pinned = false AND p.id < :cursor" + VISIBLE + " ORDER BY p.id DESC")
    List<Post> findByBoardTypeAndPinnedFalseAndIdLessThanOrderByIdDesc(@Param("boardType") BoardType boardType, @Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND p.pinned = true" + VISIBLE + " ORDER BY p.createdAt DESC")
    List<Post> findByBoardTypeAndPinnedTrueOrderByCreatedAtDesc(@Param("boardType") BoardType boardType, Pageable pageable);

    // id를 최종 타이브레이커로 둬 페이지 경계에서 중복/누락 방지
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE p.boardType = :boardType" + VISIBLE
            + " ORDER BY p.likeCount DESC, p.createdAt DESC, p.id DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.boardType = :boardType" + VISIBLE)
    Page<Post> findByBoardTypeOrderByLikeCountDescCreatedAtDescIdDesc(@Param("boardType") BoardType boardType, Pageable pageable);

    // ── 검색 (사용자 통합 검색 — FULLTEXT ngram) ────────────────────────────
    @Query(value = "SELECT id FROM post WHERE MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE) "
            + "AND deleted_at IS NULL AND blinded = false ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM post WHERE MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE) "
            + "AND deleted_at IS NULL AND blinded = false",
           nativeQuery = true)
    Page<Long> searchTitleIds(@Param("kw") String kw, Pageable pageable);

    @Query(value = "SELECT id FROM post WHERE board_type = :boardType AND MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE) "
            + "AND deleted_at IS NULL AND blinded = false ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM post WHERE board_type = :boardType AND MATCH(title) AGAINST (CONCAT('\"', REPLACE(:kw, '\"', ''), '\"') IN BOOLEAN MODE) "
            + "AND deleted_at IS NULL AND blinded = false",
           nativeQuery = true)
    Page<Long> searchTitleIdsByBoardType(@Param("boardType") String boardType, @Param("kw") String kw, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.id IN :ids" + VISIBLE)
    List<Post> findAllWithAssociationsByIdIn(@Param("ids") List<Long> ids);

    default Page<Post> searchPostsByTitleFullText(String kw, Pageable pageable) {
        return reorderByFullTextMatch(searchTitleIds(kw, stripSort(pageable)), pageable);
    }

    default Page<Post> searchPostsByBoardTypeAndTitleFullText(BoardType boardType, String kw, Pageable pageable) {
        return reorderByFullTextMatch(searchTitleIdsByBoardType(boardType.name(), kw, stripSort(pageable)), pageable);
    }

    // native 쿼리는 ORDER BY가 이미 SQL에 고정돼 있어, Pageable의 property Sort가 컬럼명으로
    // 오인돼 "Unknown column" 에러를 낼 수 있다 — 페이지 번호/크기만 남기고 Sort는 제거한다.
    private static Pageable stripSort(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private Page<Post> reorderByFullTextMatch(Page<Long> idsPage, Pageable pageable) {
        Map<Long, Post> byId = findAllWithAssociationsByIdIn(idsPage.getContent()).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> ordered = idsPage.getContent().stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, idsPage.getTotalElements());
    }

    // ── 검색 (관리자 필터) — 삭제·블라인드 제외 (현재 가시성 유지) ───────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!'" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(@Param("kw") String kw, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!'" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(@Param("boardType") BoardType boardType, @Param("kw") String kw, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist IS NOT NULL" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist IS NOT NULL AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!'" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIsNotNullAndTitleLikeOrderByCreatedAtDesc(@Param("kw") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist.id = :artistId" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIdOrderByCreatedAtDesc(@Param("artistId") Long artistId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist.id = :artistId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!'" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByArtistIdAndTitleLikeOrderByCreatedAtDesc(@Param("artistId") Long artistId, @Param("kw") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival IS NOT NULL" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival IS NOT NULL AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!'" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIsNotNullAndTitleLikeOrderByCreatedAtDesc(@Param("kw") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival.id = :festivalId" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIdOrderByCreatedAtDesc(@Param("festivalId") Long festivalId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival.id = :festivalId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) ESCAPE '!'" + VISIBLE + " ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalIdAndTitleLikeOrderByCreatedAtDesc(@Param("festivalId") Long festivalId, @Param("kw") String keyword, Pageable pageable);

    // ── 관리자 배치 카운트 / 통계 (삭제·블라인드 제외 — 현재 가시성 유지) ────
    @Query("SELECT p.user.id, COUNT(p) FROM Post p WHERE p.user.id IN :userIds" + VISIBLE + " GROUP BY p.user.id")
    List<Object[]> countGroupByUserId(@Param("userIds") List<Long> userIds);

    @Query("SELECT COUNT(p) FROM Post p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :word, '%')) ESCAPE '!' "
            + "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :word, '%')) ESCAPE '!')" + VISIBLE)
    long countByTitleOrContentContaining(@Param("word") String word);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt > :since" + VISIBLE)
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :start AND p.createdAt < :end" + VISIBLE)
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', p.createdAt), COUNT(p) FROM Post p "
            + "WHERE p.createdAt >= :from AND p.createdAt < :to" + VISIBLE
            + " GROUP BY FUNCTION('DATE', p.createdAt)")
    List<Object[]> countGroupByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.artist.id = :artistId AND p.createdAt >= :since" + VISIBLE)
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    /** 벌크 랭킹용: [artistId, postCount, likeSum] */
    @Query("SELECT p.artist.id, COUNT(p), COALESCE(SUM(p.likeCount), 0) "
            + "FROM Post p WHERE p.artist IS NOT NULL AND p.createdAt >= :since" + VISIBLE
            + " GROUP BY p.artist.id")
    List<Object[]> countAndSumByArtistSince(@Param("since") LocalDateTime since);

    // ── 댓글 카운트 ─────────────────────────────────────────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    void decrementCommentCount(@Param("postId") Long postId);

    // ── 조회수 카운트 ──────────────────────────────────────────────────────
    // 적중 행 수를 반환해 호출부가 별도 존재 확인 SELECT 없이 404를 판단한다.
    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    // ── Soft delete / 휴지통 / 블라인드 목록 (관리자용) ─────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET deleted_at = NOW() WHERE id IN (:ids)", nativeQuery = true)
    void softDeleteByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM post WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findSoftDeleted(@Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restore(@Param("id") Long id);

    // 블라인드된 글은 공개 목록/검색에 안 보이므로 관리자 전용 목록을 제공한다.
    @Query(value = "SELECT * FROM post WHERE blinded = true AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findBlinded(@Param("limit") int limit);
}
