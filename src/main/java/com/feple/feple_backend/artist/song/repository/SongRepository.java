package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByArtistIdOrderByIdDesc(Long artistId);
    boolean existsByYoutubeVideoIdAndArtistId(String youtubeVideoId, Long artistId);

    @Query("SELECT s.artist.id, COUNT(s) FROM Song s GROUP BY s.artist.id")
    List<Object[]> countGroupedByArtist();
}
