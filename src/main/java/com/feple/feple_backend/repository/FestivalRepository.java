package com.feple.feple_backend.repository;

import com.feple.feple_backend.domain.festival.Festival;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

}
