package com.feple.feple_backend.festival.suggestion.repository;

import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestion;
import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestionStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FestivalSuggestionRepository extends JpaRepository<FestivalSuggestion, Long> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM FestivalSuggestion s " +
           "WHERE s.userId = :userId AND LOWER(s.festivalName) = LOWER(:festivalName) AND s.status = :status")
    boolean existsByUserIdAndFestivalNameIgnoreCaseAndStatus(
            @Param("userId") Long userId,
            @Param("festivalName") String festivalName,
            @Param("status") FestivalSuggestionStatus status);

    List<FestivalSuggestion> findByStatusOrderByCreatedAtDesc(FestivalSuggestionStatus status);

    Page<FestivalSuggestion> findByStatusOrderByCreatedAtDesc(FestivalSuggestionStatus status, Pageable pageable);

    long countByStatus(FestivalSuggestionStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalSuggestion s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
