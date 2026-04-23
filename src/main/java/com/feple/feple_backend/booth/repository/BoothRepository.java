package com.feple.feple_backend.booth.repository;

import com.feple.feple_backend.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {
    List<Booth> findByFestivalId(Long festivalId);

    @Modifying
    @Query("DELETE FROM Booth b WHERE b.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
