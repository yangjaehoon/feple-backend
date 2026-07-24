package com.feple.feple_backend.artistfollow.repository;

import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {

    @Query("SELECT CASE WHEN COUNT(af) > 0 THEN TRUE ELSE FALSE END FROM ArtistFollow af WHERE af.user.id = :userId AND af.artist.id = :artistId")
    boolean existsByUserIdAndArtistId(@Param("userId") Long userId, @Param("artistId") Long artistId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistFollow af WHERE af.user.id = :userId AND af.artist.id = :artistId")
    int deleteByUserIdAndArtistId(@Param("userId") Long userId, @Param("artistId") Long artistId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE artist SET follower_count = GREATEST(follower_count - 1, 0) WHERE id IN (SELECT artist_id FROM artist_follow WHERE user_id = :userId)", nativeQuery = true)
    void decrementFollowerCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistFollow af WHERE af.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistFollow af WHERE af.artist.id = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);

    // artist JOIN FETCH — getFollowedArtists()에서 follow.getArtist() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFollow af JOIN FETCH af.artist WHERE af.user.id = :userId")
    List<ArtistFollow> findByUserId(@Param("userId") Long userId);

    @Query("SELECT af FROM ArtistFollow af JOIN FETCH af.artist WHERE af.user.id = :userId")
    List<ArtistFollow> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // user JOIN FETCH — notifyNewFestivalForArtist()에서 f.getUser() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFollow af JOIN FETCH af.user WHERE af.artist.id = :artistId")
    List<ArtistFollow> findByArtistId(@Param("artistId") Long artistId);

    /** 벌크 랭킹용: [artistId, followCount] */
    @Query("SELECT af.artist.id, COUNT(af) FROM ArtistFollow af " +
           "WHERE af.createdAt >= :since GROUP BY af.artist.id")
    List<Object[]> countByArtistSince(@Param("since") LocalDateTime since);

    /** 아티스트별 팔로워 일괄 조회용: [artistId, userId] — 페스티벌별 그룹핑은 호출부에서 수행 */
    @Query("SELECT af.artist.id, af.user.id FROM ArtistFollow af WHERE af.artist.id IN :artistIds")
    List<Object[]> findArtistIdAndUserIdByArtistIdIn(@Param("artistIds") List<Long> artistIds);
}
