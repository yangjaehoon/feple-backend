package com.feple.feple_backend.repository;

import com.feple.feple_backend.domain.comment.Comment;
import com.feple.feple_backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    List<Comment> findByUser(User user);
    long countByUser(User user);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.artist.id = :artistId AND c.createdAt >= :since")
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);
}
