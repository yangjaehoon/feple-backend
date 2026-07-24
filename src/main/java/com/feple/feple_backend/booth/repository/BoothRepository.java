package com.feple.feple_backend.booth.repository;

import com.feple.feple_backend.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface BoothRepository extends JpaRepository<Booth, Long> {
    @Query("SELECT b FROM Booth b WHERE b.festival.id = :festivalId")
    List<Booth> findByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Booth b WHERE b.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
