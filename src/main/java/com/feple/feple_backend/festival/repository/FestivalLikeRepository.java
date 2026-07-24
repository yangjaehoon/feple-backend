package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.FestivalLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface FestivalLikeRepository extends JpaRepository<FestivalLike, Long> {

    @Query("SELECT fl FROM FestivalLike fl WHERE fl.user.id = :userId AND fl.festival.id = :festivalId")
    Optional<FestivalLike> findByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    @Query("SELECT CASE WHEN COUNT(fl) > 0 THEN TRUE ELSE FALSE END FROM FestivalLike fl WHERE fl.user.id = :userId AND fl.festival.id = :festivalId")
    boolean existsByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    // festival JOIN FETCH — getLikedFestivals()에서 like.getFestival() 접근 시 N+1 방지
    @Query("SELECT fl FROM FestivalLike fl JOIN FETCH fl.festival WHERE fl.user.id = :userId")
    List<FestivalLike> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE festival SET like_count = GREATEST(like_count - 1, 0) WHERE id IN (SELECT festival_id FROM festival_like WHERE user_id = :userId)", nativeQuery = true)
    void decrementFestivalLikeCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalLike fl WHERE fl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalLike fl WHERE fl.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalLike fl WHERE fl.user.id = :userId AND fl.festival.id = :festivalId")
    int deleteByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);
}
