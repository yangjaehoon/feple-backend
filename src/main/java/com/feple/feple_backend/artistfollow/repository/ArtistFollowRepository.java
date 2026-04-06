package com.feple.feple_backend.artistfollow.repository;

import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {
    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
    Optional<ArtistFollow> findByUserIdAndArtistId(Long userId, Long artistId);
    long countByArtistId(Long artistId);
    List<ArtistFollow> findByUserId(Long userId);
    long countByArtistIdAndCreatedAtAfter(Long artistId, LocalDateTime since);
}
