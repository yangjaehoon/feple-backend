package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRequestRepository extends JpaRepository<SongRequest, Long> {

    @Query("SELECT sr FROM SongRequest sr WHERE sr.artist.id = :artistId AND sr.status = :status ORDER BY sr.createdAt DESC")
    List<SongRequest> findByArtistIdAndStatusOrderByCreatedAtDesc(@Param("artistId") Long artistId, @Param("status") SongRequestStatus status);

    @Query("SELECT sr FROM SongRequest sr WHERE sr.artist.id = :artistId AND sr.userId = :userId ORDER BY sr.createdAt DESC")
    List<SongRequest> findByArtistIdAndUserIdOrderByCreatedAtDesc(@Param("artistId") Long artistId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN TRUE ELSE FALSE END FROM SongRequest sr " +
           "WHERE sr.artist.id = :artistId AND sr.userId = :userId " +
           "AND LOWER(sr.songTitle) = LOWER(:songTitle) AND sr.status = :status")
    boolean existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
            @Param("artistId") Long artistId, @Param("userId") Long userId,
            @Param("songTitle") String songTitle, @Param("status") SongRequestStatus status);

    List<SongRequest> findByStatusOrderByCreatedAtDesc(SongRequestStatus status, Pageable pageable);

    @Query("SELECT sr FROM SongRequest sr WHERE " +
           "(:status IS NULL OR sr.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(sr.songTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(sr.artist.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY sr.createdAt DESC")
    Page<SongRequest> findWithFilters(@Param("status") SongRequestStatus status,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    long countByStatus(SongRequestStatus status);

    @Query("SELECT sr FROM SongRequest sr WHERE sr.userId = :userId ORDER BY sr.createdAt DESC")
    List<SongRequest> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
