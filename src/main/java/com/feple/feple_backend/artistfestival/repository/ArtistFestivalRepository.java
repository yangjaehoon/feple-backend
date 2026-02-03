package com.feple.feple_backend.artistfestival.repository;

import com.feple.feple_backend.artistfestival.domain.ArtistFestival;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistFestivalRepository extends JpaRepository<ArtistFestival, Long> {

    List<ArtistFestival> findByFestivalId(Long festivalId);

    boolean existsByFestivalIdAndArtistId(Long festivalId, Long artistId);

    long deleteByFestivalId(Long festivalId);

}
