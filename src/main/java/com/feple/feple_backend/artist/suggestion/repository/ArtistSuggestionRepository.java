package com.feple.feple_backend.artist.suggestion.repository;

import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistSuggestionRepository extends JpaRepository<ArtistSuggestion, Long> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM ArtistSuggestion s " +
           "WHERE s.userId = :userId AND LOWER(s.artistName) = LOWER(:artistName) AND s.status = :status")
    boolean existsByUserIdAndArtistNameIgnoreCaseAndStatus(
            @Param("userId") Long userId,
            @Param("artistName") String artistName,
            @Param("status") ArtistSuggestionStatus status);

    List<ArtistSuggestion> findByStatusOrderByCreatedAtDesc(ArtistSuggestionStatus status);

    Page<ArtistSuggestion> findByStatusOrderByCreatedAtDesc(ArtistSuggestionStatus status, Pageable pageable);

    long countByStatus(ArtistSuggestionStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistSuggestion s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
