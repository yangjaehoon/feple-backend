package com.feple.feple_backend.repository;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.artist.domain.ArtistImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistImageRepository extends JpaRepository<ArtistImage, Long> {
    List<ArtistImage> findByArtist(Artist artist);
}

