package com.feple.feple_backend.artistfollow.repository;

import com.feple.feple_backend.artistfollow.domain.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {
    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
    Optional<ArtistFollow> findByUserIdAndArtistId(Long userId, Long artistId);
    long countByArtistId(Long artistId);
}
