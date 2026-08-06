package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    Page<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId, Pageable pageable);

    // @SQLRestriction을 우회하는 네이티브 쿼리 — 블라인드된 댓글도 관리자는 검토할 수 있어야 함
    @Query(value = "SELECT * FROM comment WHERE post_id = :postId AND deleted_at IS NULL ORDER BY created_at ASC LIMIT :limit",
           nativeQuery = true)
    List<Comment> findByPostIdIgnoringBlindOrderByCreatedAtAsc(@Param("postId") Long postId, @Param("limit") int limit);

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

    // 관리자 상세 — userId 직접 사용 (User 엔티티 사전 조회 불필요)
    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query(value = "SELECT c FROM Comment c WHERE c.user.id = :userId ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId")
    Page<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUser(User user);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt >= :start AND c.createdAt < :end AND c.deletedAt IS NULL")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', c.createdAt), COUNT(c) FROM Comment c " +
           "WHERE c.createdAt >= :from AND c.createdAt < :to GROUP BY FUNCTION('DATE', c.createdAt)")
    List<Object[]> countPerDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

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

    // 벌크 DELETE는 @SQLDelete를 우회한 하드 삭제 — post 캐스케이드 전용
    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    // ── 좋아요 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementLikeCount(@Param("id") Long id);

    // ── 블라인드 관리자용 ──────────────────────────────────────────────────────
    // @SQLRestriction을 우회하는 네이티브 쿼리 — 블라인드된 댓글도 신고 접수·관리자 삭제는 가능해야 함
    @Query(value = "SELECT * FROM comment WHERE id = :id", nativeQuery = true)
    java.util.Optional<Comment> findByIdIgnoringRestrictions(@Param("id") Long id);

    // deleteById()는 findById()로 먼저 존재를 확인하는데 blinded=true면 @SQLRestriction에 걸려
    // 못 찾으므로, 블라인드 여부와 무관하게 소프트 삭제(@SQLDelete와 동일한 SQL)하는 벌크 쿼리로 우회한다.
    @Modifying
    @Transactional
    @Query(value = "UPDATE comment SET deleted_at = NOW() WHERE id = :id", nativeQuery = true)
    void softDeleteById(@Param("id") Long id);
}
