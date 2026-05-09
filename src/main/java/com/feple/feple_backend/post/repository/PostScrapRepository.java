package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostScrap;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    @Query("SELECT ps FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    Optional<PostScrap> findByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN TRUE ELSE FALSE END FROM PostScrap ps WHERE ps.user.id = :userId AND ps.post.id = :postId")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    @Query("SELECT ps FROM PostScrap ps WHERE ps.user.id = :userId ORDER BY ps.id DESC")
    List<PostScrap> findByUserIdOrderByIdDesc(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PostScrap ps WHERE ps.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    void deleteByUser(User user);
}
