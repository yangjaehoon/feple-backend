package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SongRequestRepository extends JpaRepository<SongRequest, Long> {

    List<SongRequest> findByArtistIdAndStatusOrderByCreatedAtDesc(Long artistId, SongRequestStatus status);

    List<SongRequest> findByArtistIdAndUserIdOrderByCreatedAtDesc(Long artistId, Long userId);

    boolean existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
            Long artistId, Long userId, String songTitle, SongRequestStatus status);

    List<SongRequest> findByStatusOrderByCreatedAtDesc(SongRequestStatus status, Pageable pageable);

    long countByStatus(SongRequestStatus status);
}
