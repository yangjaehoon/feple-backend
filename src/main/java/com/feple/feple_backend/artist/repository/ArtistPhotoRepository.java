package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.domain.ArtistPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistPhotoRepository extends JpaRepository<ArtistPhoto, Long> {
    List<ArtistPhoto> findByArtistIdOrderByIdDesc(Long artistId);
}