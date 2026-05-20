package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRequestRepository extends JpaRepository<SongRequest, Long> {

    List<SongRequest> findByArtistIdAndStatusOrderByCreatedAtDesc(Long artistId, SongRequestStatus status);

    List<SongRequest> findByArtistIdAndUserIdOrderByCreatedAtDesc(Long artistId, Long userId);

    @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN TRUE ELSE FALSE END FROM SongRequest sr " +
           "WHERE sr.artist.id = :artistId AND sr.userId = :userId " +
           "AND LOWER(sr.songTitle) = LOWER(:songTitle) AND sr.status = :status")
    boolean existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
            @Param("artistId") Long artistId, @Param("userId") Long userId,
            @Param("songTitle") String songTitle, @Param("status") SongRequestStatus status);

    List<SongRequest> findByStatusOrderByCreatedAtDesc(SongRequestStatus status, Pageable pageable);

    long countByStatus(SongRequestStatus status);
}
