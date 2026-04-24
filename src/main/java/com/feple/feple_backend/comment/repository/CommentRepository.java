package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    // post/artist/festival/user JOIN FETCH — MyCommentResponseDto::from에서 N+1 방지
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.post p " +
           "JOIN FETCH p.user " +
           "LEFT JOIN FETCH p.artist " +
           "LEFT JOIN FETCH p.festival " +
           "WHERE c.user = :user " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findByUser(@Param("user") User user);

    long countByUser(User user);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.artist.id = :artistId AND c.createdAt >= :since")
    long countByArtistAndSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    /** 벌크 랭킹용: [artistId, commentCount] */
    @Query("SELECT c.post.artist.id, COUNT(c) " +
           "FROM Comment c WHERE c.post.artist IS NOT NULL AND c.createdAt >= :since " +
           "GROUP BY c.post.artist.id")
    List<Object[]> countByArtistSince(@Param("since") LocalDateTime since);
}
