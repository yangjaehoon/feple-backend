package com.feple.feple_backend.booth.repository;

import com.feple.feple_backend.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {
    List<Booth> findByFestivalId(Long festivalId);
}
