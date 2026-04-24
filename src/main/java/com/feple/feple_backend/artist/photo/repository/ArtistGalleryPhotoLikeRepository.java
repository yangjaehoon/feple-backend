package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArtistGalleryPhotoLikeRepository extends JpaRepository<ArtistGalleryPhotoLike, Long> {

    boolean existsByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    Optional<ArtistGalleryPhotoLike> findByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    long countByArtistPhotoId(Long artistPhotoId);

    long deleteByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);
}
