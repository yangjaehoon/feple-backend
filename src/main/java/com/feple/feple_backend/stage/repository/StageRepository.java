package com.feple.feple_backend.stage.repository;

import com.feple.feple_backend.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByFestivalIdOrderByName(Long festivalId);
    void deleteByFestivalId(Long festivalId);
}
