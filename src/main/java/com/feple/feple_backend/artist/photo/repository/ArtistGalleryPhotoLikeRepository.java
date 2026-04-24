package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ArtistGalleryPhotoLikeRepository extends JpaRepository<ArtistGalleryPhotoLike, Long> {

    boolean existsByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    Optional<ArtistGalleryPhotoLike> findByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    long countByArtistPhotoId(Long artistPhotoId);

    long deleteByArtistPhotoIdAndUserId(Long artistPhotoId, Long userId);

    @Query("SELECT apl.artistPhotoId FROM ArtistGalleryPhotoLike apl WHERE apl.userId = :userId AND apl.artistPhotoId IN :photoIds")
    Set<Long> findLikedPhotoIds(@Param("userId") Long userId, @Param("photoIds") List<Long> photoIds);
}
