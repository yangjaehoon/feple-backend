package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByArtistIdOrderByIdDesc(Long artistId);
    boolean existsByYoutubeVideoIdAndArtistId(String youtubeVideoId, Long artistId);
}
