package com.feple.feple_backend.artistfollow.repository;

import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {
    boolean existsByUserIdAndArtistId(Long userId, Long artistId);

    Optional<ArtistFollow> findByUserIdAndArtistId(Long userId, Long artistId);

    void deleteByUserIdAndArtistId(Long userId, Long artistId);

    long countByArtistId(Long artistId);

    List<ArtistFollow> findByUserId(Long userId);

    List<ArtistFollow> findByArtistId(Long artistId);

    long countByArtistIdAndCreatedAtAfter(Long artistId, LocalDateTime since);

    /** 벌크 랭킹용: [artistId, followCount] */
    @org.springframework.data.jpa.repository.Query(
        "SELECT af.artist.id, COUNT(af) FROM ArtistFollow af " +
        "WHERE af.createdAt >= :since GROUP BY af.artist.id")
    List<Object[]> countByArtistSince(@org.springframework.data.repository.query.Param("since") LocalDateTime since);
}
