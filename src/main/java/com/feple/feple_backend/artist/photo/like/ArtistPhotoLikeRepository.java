package com.feple.feple_backend.artist.photo.like;

import com.feple.feple_backend.artist.entity.ArtistPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArtistPhotoLikeRepository extends JpaRepository<ArtistPhotoLike, Long> {

    boolean existsByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    Optional<ArtistPhotoLike> findByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    long countByArtistPhotoId(Long artistPhotoId);

    long deleteByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);
}