package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN TRUE ELSE FALSE END FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    boolean existsByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    void deleteByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    List<Long> findLikedCommentIdsByUserAndCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds);
}
