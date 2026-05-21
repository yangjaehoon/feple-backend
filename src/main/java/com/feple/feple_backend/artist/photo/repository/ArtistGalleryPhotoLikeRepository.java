package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ArtistGalleryPhotoLikeRepository extends JpaRepository<ArtistGalleryPhotoLike, Long> {

    @Query("SELECT CASE WHEN COUNT(apl) > 0 THEN TRUE ELSE FALSE END FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId AND apl.user.id = :userId")
    boolean existsByPhoto_IdAndUser_Id(@Param("photoId") Long photoId, @Param("userId") Long userId);

    Optional<ArtistGalleryPhotoLike> findByPhoto_IdAndUser_Id(Long photoId, Long userId);

    long countByPhoto_Id(Long photoId);

    @Query("SELECT apl.photo.id FROM ArtistGalleryPhotoLike apl WHERE apl.user.id = :userId AND apl.photo.id IN :photoIds")
    Set<Long> findLikedPhotoIds(@Param("userId") Long userId, @Param("photoIds") List<Long> photoIds);

    @Modifying
    @Query("DELETE FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId AND apl.user.id = :userId")
    int deleteByPhotoIdAndUserId(@Param("photoId") Long photoId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId")
    void deleteByPhotoId(@Param("photoId") Long photoId);
}
