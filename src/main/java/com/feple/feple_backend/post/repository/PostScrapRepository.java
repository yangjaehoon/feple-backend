package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostScrap;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    Optional<PostScrap> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @EntityGraph(attributePaths = {"post", "post.user", "post.artist", "post.festival"})
    List<PostScrap> findByUserIdOrderByIdDesc(Long userId);

    void deleteByPostId(Long postId);

    void deleteByUser(User user);
}
