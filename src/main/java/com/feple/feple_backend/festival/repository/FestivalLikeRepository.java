package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.FestivalLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FestivalLikeRepository extends JpaRepository<FestivalLike, Long> {
    Optional<FestivalLike> findByUserIdAndFestivalId(Long userId, Long festivalId);
    boolean existsByUserIdAndFestivalId(Long userId, Long festivalId);
    List<FestivalLike> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM FestivalLike fl WHERE fl.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
