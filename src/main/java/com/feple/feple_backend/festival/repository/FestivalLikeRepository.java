package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.FestivalLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FestivalLikeRepository extends JpaRepository<FestivalLike, Long> {
    Optional<FestivalLike> findByUserIdAndFestivalId(Long userId, Long festivalId);
    boolean existsByUserIdAndFestivalId(Long userId, Long festivalId);
}
