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
    @Query("SELECT p FROM Post p WHERE p.festival = :festival ORDER BY p.createdAt DESC")
    Page<Post> findByFestivalOrderByCreatedAtDesc(@Param("festival") Festival festival, Pageable pageable);

    // ── 내 게시글 (N+1: user/artist/festival 모두 접근) ──────────────────────
    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.artist LEFT JOIN FETCH p.festival WHERE p.user = :user")
    List<Post> findByUser(@Param("user") User user);

    long countByUser(User user);

    // ── 핫 게시글 (N+1: user/artist/festival 접근) ───────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC")
    List<Post> findHotPosts(@Param("since") LocalDateTime since, Pageable pageable);

    // ── 게시판 타입별 (N+1: user/artist/festival 접근) ───────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    // ── 검색 (관리자) ────────────────────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "artist", "festival"})
    Page<Post> findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(BoardType boardType, String title, Pageable pageable);

    // ── 통계 ─────────────────────────────────────────────────────────────────
    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.artist.id = :artistId AND p.createdAt >= :since")
    long sumLikeCountByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.artist.id = :artistId AND p.createdAt >= :since")
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    /** 벌크 랭킹용: [artistId, postCount, likeSum] */
    @Query("SELECT p.artist.id, COUNT(p), COALESCE(SUM(p.likeCount), 0) " +
           "FROM Post p WHERE p.artist IS NOT NULL AND p.createdAt >= :since " +
           "GROUP BY p.artist.id")
    List<Object[]> countAndSumByArtistSince(@Param("since") LocalDateTime since);

    // ── 좋아요 카운트 ─────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);
}
