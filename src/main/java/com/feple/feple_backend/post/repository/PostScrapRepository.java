package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostScrap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    @Query("SELECT ps FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    Optional<PostScrap> findByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN TRUE ELSE FALSE END FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query("SELECT ps FROM PostScrap ps WHERE ps.user.id = :userId ORDER BY ps.id DESC")
    List<PostScrap> findByUserIdOrderByIdDesc(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query("SELECT ps FROM PostScrap ps WHERE ps.user.id = :userId ORDER BY ps.id DESC")
    List<PostScrap> findByUserIdOrderByIdDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(ps) FROM PostScrap ps WHERE ps.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE post SET scrap_count = GREATEST(scrap_count - 1, 0) WHERE id IN (SELECT post_id FROM post_scrap WHERE user_id = :userId)", nativeQuery = true)
    void decrementPostScrapCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostScrap ps WHERE ps.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
