package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.domain.Festival;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.nio.channels.FileChannel;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    Page<Festival> findAllByOrderByStartDateDesc(Pageable pageable);
}
