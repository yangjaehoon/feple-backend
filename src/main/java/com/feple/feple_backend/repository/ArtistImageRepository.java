package com.feple.feple_backend.repository;

import com.feple.feple_backend.domain.artist.Artist;
import com.feple.feple_backend.domain.artist.ArtistImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistImageRepository extends JpaRepository<ArtistImage, Long> {
    List<ArtistImage> findByArtist(Artist artist);
}

