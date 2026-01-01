package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
