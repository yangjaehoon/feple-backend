package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByBoardType(BoardType boardType);
    List<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType);
    List<Post> findByArtist(Artist artist);
    List<Post> findByArtistOrderByCreatedAtDesc(Artist artist);
    org.springframework.data.domain.Page<Post> findByArtistOrderByCreatedAtDesc(Artist artist, Pageable pageable);
    List<Post> findByFestival(Festival festival);
    List<Post> findByFestivalOrderByCreatedAtDesc(Festival festival);
    org.springframework.data.domain.Page<Post> findByFestivalOrderByCreatedAtDesc(Festival festival, Pageable pageable);
    List<Post> findByUser(User user);
    long countByUser(User user);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC")
    List<Post> findHotPosts(@Param("since") LocalDateTime since, Pageable pageable);

    org.springframework.data.domain.Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    org.springframework.data.domain.Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

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

    org.springframework.data.domain.Page<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title, Pageable pageable);

    org.springframework.data.domain.Page<Post> findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(BoardType boardType, String title, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);
}
