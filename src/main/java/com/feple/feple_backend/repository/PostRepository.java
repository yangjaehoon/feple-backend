package com.feple.feple_backend.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.festival.entity.Festival;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByBoardType(BoardType boardType);
    List<Post> findByArtist(Artist artist);
    List<Post> findByFestival(Festival festival);
}
