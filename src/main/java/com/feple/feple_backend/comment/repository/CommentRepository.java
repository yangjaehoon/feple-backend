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

/**
 * 공개 조회 메서드는 삭제(deleted_at)·블라인드(blinded) 행을 명시적으로 제외한다.
 * 관리자·본인·신고·캐스케이드 경로는 기본 {@code findById}/{@code deleteById}로 모든 행을 다룬다.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ── 공개 조회 (삭제·블라인드 제외) ────────────────────────────────────────
    @EntityGraph(attributePaths = {"user", "mentionedUser"})
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId "
            + "AND c.deletedAt IS NULL AND c.blinded = false ORDER BY c.createdAt ASC")
    Page<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId, Pageable pageable);

    // 관리자 댓글 검토용 — 블라인드는 포함, 삭제만 제외
    @Query(value = "SELECT * FROM comment WHERE post_id = :postId AND deleted_at IS NULL "
            + "ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<Comment> findAdminByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId, @Param("limit") int limit);

    // 마이페이지 — 삭제·블라인드 제외
    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query(value = "SELECT c FROM Comment c WHERE c.user = :user "
            + "AND c.deletedAt IS NULL AND c.blinded = false ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.user = :user "
            + "AND c.deletedAt IS NULL AND c.blinded = false")
    Page<Comment> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    // 관리자 상세 — userId 직접 사용, 삭제·블라인드 제외 (현재 가시성 유지)
    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query(value = "SELECT c FROM Comment c WHERE c.user.id = :userId "
            + "AND c.deletedAt IS NULL AND c.blinded = false ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId "
            + "AND c.deletedAt IS NULL AND c.blinded = false")
    Page<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.deletedAt IS NULL AND c.blinded = false")
    long countByUserId(@Param("userId") Long userId);

    // 완전 삭제(hardDelete) 선조건 — 소프트 삭제·블라인드된 댓글도 comment.user_id FK로 users 행을
    // 잡고 있으므로, 가시성 필터 없이 "작성 이력이 하나라도 있는지"를 본다.
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Comment c WHERE c.user.id = :userId")
    boolean existsAnyByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt >= :start AND c.createdAt < :end "
            + "AND c.deletedAt IS NULL AND c.blinded = false")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', c.createdAt), COUNT(c) FROM Comment c "
            + "WHERE c.createdAt >= :from AND c.createdAt < :to AND c.deletedAt IS NULL AND c.blinded = false "
            + "GROUP BY FUNCTION('DATE', c.createdAt)")
    List<Object[]> countPerDate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(c) FROM Comment c WHERE LOWER(c.content) LIKE LOWER(CONCAT('%', :word, '%')) ESCAPE '!' "
            + "AND c.deletedAt IS NULL AND c.blinded = false")
    long countByContentContaining(@Param("word") String word);

    @Query("SELECT c.user.id, COUNT(c) FROM Comment c WHERE c.user.id IN :userIds "
            + "AND c.deletedAt IS NULL AND c.blinded = false GROUP BY c.user.id")
    List<Object[]> countGroupByUserId(@Param("userIds") List<Long> userIds);

    // 벌크 DELETE는 @SQLDelete를 우회한 하드 삭제 — post 캐스케이드 전용 (삭제·블라인드 무관 전부 제거)
    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    // 회원 완전 삭제 전용 — 다른 유저의 댓글이 이 유저를 멘션하고 있으면 FK 때문에 users 행을
    // 지울 수 없으므로 멘션 참조만 끊는다(댓글 본문은 그대로 유지).
    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.mentionedUser = null WHERE c.mentionedUser.id = :userId")
    void clearMentionsByUserId(@Param("userId") Long userId);

    // ── 좋아요 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    // clearAutomatically는 쓰지 않는다(영속성 컨텍스트 전체 clear의 부작용). 증감 직후 최신 값이
    // 필요하면 findLikeCountById로 스칼라 조회한다.
    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementLikeCount(@Param("id") Long id);

    /** 카운터 증감 직후 최신 좋아요 수만 스칼라로 다시 읽는다. */
    @Query("SELECT c.likeCount FROM Comment c WHERE c.id = :id")
    Integer findLikeCountById(@Param("id") Long id);
}
