package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id = :postId")
    Optional<PostLike> findByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN TRUE ELSE FALSE END FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id = :postId")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id = :postId")
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET like_count = GREATEST(like_count - 1, 0) WHERE id IN (SELECT post_id FROM post_like WHERE user_id = :userId)", nativeQuery = true)
    void decrementPostLikeCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT pl.post FROM PostLike pl " +
           "JOIN FETCH pl.post.user " +
           "LEFT JOIN FETCH pl.post.artist " +
           "LEFT JOIN FETCH pl.post.festival " +
           "WHERE pl.user.id = :userId ORDER BY pl.id DESC")
    List<Post> findPostsByUserId(@Param("userId") Long userId);

    @Query("SELECT pl.post FROM PostLike pl " +
           "JOIN FETCH pl.post.user " +
           "LEFT JOIN FETCH pl.post.artist " +
           "LEFT JOIN FETCH pl.post.festival " +
           "WHERE pl.user.id = :userId ORDER BY pl.id DESC")
    List<Post> findPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
