package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.domain.Festival;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

}
