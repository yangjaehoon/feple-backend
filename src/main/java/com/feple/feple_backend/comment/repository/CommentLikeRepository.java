package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN TRUE ELSE FALSE END FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    boolean existsByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    int deleteByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id IN (SELECT comment_id FROM comment_like WHERE user_id = :userId)", nativeQuery = true)
    void decrementCommentLikeCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    List<Long> findLikedCommentIdsByUserAndCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds);
}
