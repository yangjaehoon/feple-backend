package com.feple.feple_backend.booth.repository;

import com.feple.feple_backend.booth.entity.Booth;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoothRepository extends JpaRepository<Booth, Long> {
    @Query("SELECT b FROM Booth b WHERE b.festival.id = :festivalId")
    List<Booth> findByFestivalId(@Param("festivalId") Long festivalId);
}
