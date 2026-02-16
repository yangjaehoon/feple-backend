package com.feple.feple_backend.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistImageRepository extends JpaRepository<ArtistImage, Long> {
    List<ArtistImage> findByArtist(Artist artist);
}

