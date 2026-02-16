package com.feple.feple_backend.artist.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistPhotoRepository extends JpaRepository<ArtistPhoto, Long> {
    List<ArtistPhoto> findByArtistIdOrderByIdDesc(Long artistId);

    List<ArtistPhoto> findByArtistIdOrderByLikecountDescCreatedAtDesc(Long artistId);

    ArtistPhoto findByIdAndArtistId(Long id, Long artistId);  // 또는 그냥 findById 사용

}