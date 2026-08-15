package com.feple.feple_backend.diary.repository;

import com.feple.feple_backend.diary.entity.DiaryVisibility;
import com.feple.feple_backend.diary.entity.FestivalDiary;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FestivalDiaryRepository extends JpaRepository<FestivalDiary, Long> {

    @Query("SELECT d FROM FestivalDiary d JOIN FETCH d.festival WHERE d.user.id = :userId ORDER BY d.createdAt DESC")
    List<FestivalDiary> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT d FROM FestivalDiary d JOIN FETCH d.festival WHERE d.user.id = :userId AND d.festival.id = :festivalId ORDER BY d.createdAt DESC")
    List<FestivalDiary> findByUserIdAndFestivalIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    @Query("SELECT d FROM FestivalDiary d JOIN FETCH d.user WHERE d.festival.id = :festivalId AND d.visibility = :visibility ORDER BY d.createdAt DESC")
    Page<FestivalDiary> findByFestivalIdAndVisibilityOrderByCreatedAtDesc(
            @Param("festivalId") Long festivalId, @Param("visibility") DiaryVisibility visibility, Pageable pageable);

    @Query("SELECT d FROM FestivalDiary d JOIN FETCH d.festival WHERE d.user.id = :userId AND d.visibility = :visibility ORDER BY d.createdAt DESC")
    Page<FestivalDiary> findByUserIdAndVisibilityOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("visibility") DiaryVisibility visibility, Pageable pageable);

    @Query("SELECT d FROM FestivalDiary d JOIN FETCH d.festival WHERE d.user.id = :userId")
    List<FestivalDiary> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalDiary d WHERE d.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
