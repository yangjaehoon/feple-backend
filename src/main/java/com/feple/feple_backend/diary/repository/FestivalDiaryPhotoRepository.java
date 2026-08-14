package com.feple.feple_backend.diary.repository;

import com.feple.feple_backend.diary.entity.FestivalDiaryPhoto;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalDiaryPhotoRepository extends JpaRepository<FestivalDiaryPhoto, Long> {

    @Query("SELECT p FROM FestivalDiaryPhoto p WHERE p.diary.id = :diaryId ORDER BY p.sortOrder ASC")
    List<FestivalDiaryPhoto> findByDiaryIdOrderBySortOrder(@Param("diaryId") Long diaryId);

    @Query("SELECT p FROM FestivalDiaryPhoto p WHERE p.diary.id IN :diaryIds ORDER BY p.sortOrder ASC")
    List<FestivalDiaryPhoto> findByDiaryIdIn(@Param("diaryIds") Collection<Long> diaryIds);
}
