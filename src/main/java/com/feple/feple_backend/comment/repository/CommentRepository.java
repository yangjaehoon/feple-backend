package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.Comment;
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

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    Page<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId, Pageable pageable);

    // post/artist/festival/user JOIN FETCH — MyCommentResponseDto::from에서 N+1 방지
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.artist " +
           "LEFT JOIN FETCH p.festival " +
           "WHERE c.user = :user " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findByUser(@Param("user") User user); // 계정 삭제 등 전체 처리용

    // 마이페이지 표시용 — 최신순 정렬, 상한선 적용 (Pageable)
    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    Page<Comment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUser(User user);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    void incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.id = :commentId")
    void decrementLikeCount(@Param("commentId") Long commentId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', c.createdAt), COUNT(c) FROM Comment c " +
           "WHERE c.createdAt >= :from AND c.createdAt < :to GROUP BY FUNCTION('DATE', c.createdAt)")
    List<Object[]> countGroupByDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.artist.id = :artistId AND c.createdAt >= :since")
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    /** 벌크 랭킹용: [artistId, commentCount] */
    @Query("SELECT c.post.artist.id, COUNT(c) " +
           "FROM Comment c WHERE c.post.artist IS NOT NULL AND c.createdAt >= :since " +
           "GROUP BY c.post.artist.id")
    List<Object[]> countByArtistSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM Comment c WHERE LOWER(c.content) LIKE LOWER(CONCAT('%', :word, '%')) ESCAPE '!'")
    long countByContentContaining(@Param("word") String word);

    @Query("SELECT c.user.id, COUNT(c) FROM Comment c WHERE c.user.id IN :userIds GROUP BY c.user.id")
    List<Object[]> countGroupByUserId(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);
}
