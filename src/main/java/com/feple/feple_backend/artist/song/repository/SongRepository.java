package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface SongRepository extends JpaRepository<Song, Long> {
    @Query("SELECT s FROM Song s WHERE s.artist.id = :artistId ORDER BY s.id DESC")
    List<Song> findByArtistIdOrderByIdDesc(@Param("artistId") Long artistId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM Song s WHERE s.youtubeVideoId = :youtubeVideoId AND s.artist.id = :artistId")
    boolean existsByYoutubeVideoIdAndArtistId(@Param("youtubeVideoId") String youtubeVideoId, @Param("artistId") Long artistId);

    @Query("SELECT s.artist.id, COUNT(s) FROM Song s GROUP BY s.artist.id")
    List<Object[]> countGroupedByArtist();

    @Query("SELECT s.artist.id, COUNT(s) FROM Song s WHERE s.artist.id IN :artistIds GROUP BY s.artist.id")
    List<Object[]> countGroupedByArtistIds(@Param("artistIds") List<Long> artistIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Song s WHERE s.artist.id = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);
}
