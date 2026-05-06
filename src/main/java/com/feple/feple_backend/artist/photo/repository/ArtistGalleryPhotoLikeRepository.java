package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ArtistGalleryPhotoLikeRepository extends JpaRepository<ArtistGalleryPhotoLike, Long> {

    boolean existsByPhoto_IdAndUser_Id(Long photoId, Long userId);

    Optional<ArtistGalleryPhotoLike> findByPhoto_IdAndUser_Id(Long photoId, Long userId);

    long countByPhoto_Id(Long photoId);

    long deleteByPhoto_IdAndUser_Id(Long photoId, Long userId);

    @Query("SELECT apl.photo.id FROM ArtistGalleryPhotoLike apl WHERE apl.user.id = :userId AND apl.photo.id IN :photoIds")
    Set<Long> findLikedPhotoIds(@Param("userId") Long userId, @Param("photoIds") List<Long> photoIds);
}
