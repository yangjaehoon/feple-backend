package com.feple.feple_backend.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByBoardType(BoardType boardType);
    List<Post> findByArtist(Artist artist);
    List<Post> findByFestival(Festival festival);
    List<Post> findByUser(User user);
    long countByUser(User user);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC")
    List<Post> findHotPosts(@Param("since") LocalDateTime since, Pageable pageable);
}
