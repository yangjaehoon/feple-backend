package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ── 아티스트 게시글 ──────────────────────────────────────────────────────
    List<Post> findByBoardType(BoardType boardType);
    List<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType);
    List<Post> findByArtist(Artist artist);
    List<Post> findByArtistOrderByCreatedAtDesc(Artist artist);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.artist = :artist ORDER BY p.createdAt DESC")
    Page<Post> findByArtistOrderByCreatedAtDesc(@Param("artist") Artist artist, Pageable pageable);

    // ── 페스티벌 게시글 ──────────────────────────────────────────────────────
    List<Post> findByFestival(Festival festival);
    List<Post> findByFestivalOrderByCreatedAtDesc(Festival festival);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType IS NULL ORDER BY p.createdAt DESC")
    Page<Post> findGeneralFestivalPosts(@Param("festival") Festival festival, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival AND p.boardType = :boardType ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalAndBoardTypeOrderByCreatedAtDesc(@Param("festival") Festival festival, @Param("boardType") BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.festival = :festival ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findByFestivalOrderByLikeCountDesc(@Param("festival") Festival festival, Pageable pageable);

    // ── 내 게시글 (N+1: user/artist/festival 모두 접근) ──────────────────────
    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user")
    List<Post> findByUser(@Param("user") User user); // 계정 삭제 등 전체 처리용

    // 마이페이지 표시용 — 최신순 정렬, 상한선 적용 (Pageable)
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 관리자 상세 — userId 직접 사용 (User 엔티티 사전 조회 불필요)
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query(value = "SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    Page<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUser(User user);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // ── 핫 게시글 (N+1: user/artist/festival 접근) ───────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC")
    List<Post> findHotPosts(@Param("since") LocalDateTime since, Pageable pageable);

    // ── 게시판 타입별 (N+1: user/artist/festival 접근) ───────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    // ── 커서 기반 최신순 (id < cursor) ─────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByBoardTypeOrderByIdDesc(BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    List<Post> findByBoardTypeAndIdLessThanOrderByIdDesc(BoardType boardType, Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByBoardTypeOrderByLikeCountDescCreatedAtDesc(BoardType boardType, Pageable pageable);

    // ── 검색 (관리자) ────────────────────────────────────────────────────────
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
    long countByCreatedAtAfter(LocalDateTime since);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', p.createdAt), COUNT(p) FROM Post p " +
           "WHERE p.createdAt >= :from AND p.createdAt < :to GROUP BY FUNCTION('DATE', p.createdAt)")
    List<Object[]> countGroupByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.artist.id = :artistId AND p.createdAt >= :since")
    long sumLikeCountByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.artist.id = :artistId AND p.createdAt >= :since")
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    /** 벌크 랭킹용: [artistId, postCount, likeSum] */
    @Query("SELECT p.artist.id, COUNT(p), COALESCE(SUM(p.likeCount), 0) " +
           "FROM Post p WHERE p.artist IS NOT NULL AND p.createdAt >= :since " +
           "GROUP BY p.artist.id")
    List<Object[]> countAndSumByArtistSince(@Param("since") LocalDateTime since);

    // ── 조회수 카운트 ─────────────────────────────────────────────────────────
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Query("SELECT p.viewCount FROM Post p WHERE p.id = :postId")
    int findViewCountById(@Param("postId") Long postId);

    // ── 스크랩 카운트 (원자적 SQL UPDATE — 엔티티 dirty check 대신 사용)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.scrapCount = p.scrapCount + 1 WHERE p.id = :postId")
    void incrementScrapCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.scrapCount = p.scrapCount - 1 WHERE p.id = :postId AND p.scrapCount > 0")
    void decrementScrapCount(@Param("postId") Long postId);

    // ── 좋아요 카운트 ─────────────────────────────────────────────────────────
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);

    // ── Soft delete 관리자용 ──────────────────────────────────────────────────
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE post SET deleted_at = NOW() WHERE id IN (:ids)", nativeQuery = true)
    void softDeleteByIds(@Param("ids") List<Long> ids);

    // @SQLRestriction을 우회하는 네이티브 쿼리 — 관리자 휴지통 조회용
    @Query(value = "SELECT * FROM post WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findSoftDeleted(@Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE post SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restore(@Param("id") Long id);

}
