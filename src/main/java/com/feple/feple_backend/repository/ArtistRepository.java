package com.feple.feple_backend.repository;

import com.feple.feple_backend.domain.artist.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
