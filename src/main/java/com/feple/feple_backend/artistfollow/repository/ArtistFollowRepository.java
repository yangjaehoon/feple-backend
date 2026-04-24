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

    void deleteByUserIdAndArtistId(Long userId, Long artistId);

    long countByArtistId(Long artistId);

    // artist JOIN FETCH — getFollowedArtists()에서 follow.getArtist() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFollow af JOIN FETCH af.artist WHERE af.user.id = :userId")
    List<ArtistFollow> findByUserId(@Param("userId") Long userId);

    // user JOIN FETCH — notifyNewFestivalForArtist()에서 f.getUser() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFollow af JOIN FETCH af.user WHERE af.artist.id = :artistId")
    List<ArtistFollow> findByArtistId(@Param("artistId") Long artistId);

    long countByArtistIdAndCreatedAtAfter(Long artistId, LocalDateTime since);

    /** 벌크 랭킹용: [artistId, followCount] */
    @Query("SELECT af.artist.id, COUNT(af) FROM ArtistFollow af " +
           "WHERE af.createdAt >= :since GROUP BY af.artist.id")
    List<Object[]> countByArtistSince(@Param("since") LocalDateTime since);
}
