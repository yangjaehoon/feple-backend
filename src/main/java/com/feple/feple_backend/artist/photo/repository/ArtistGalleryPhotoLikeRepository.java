package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistGalleryPhotoLikeRepository extends JpaRepository<ArtistGalleryPhotoLike, Long> {

    @Query("SELECT CASE WHEN COUNT(apl) > 0 THEN TRUE ELSE FALSE END FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId AND apl.user.id = :userId")
    boolean existsByPhoto_IdAndUser_Id(@Param("photoId") Long photoId, @Param("userId") Long userId);

    @Query("SELECT apl FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId AND apl.user.id = :userId")
    Optional<ArtistGalleryPhotoLike> findByPhoto_IdAndUser_Id(@Param("photoId") Long photoId, @Param("userId") Long userId);

    @Query("SELECT COUNT(apl) FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId")
    long countByPhoto_Id(@Param("photoId") Long photoId);

    @Query("SELECT apl.photo.id FROM ArtistGalleryPhotoLike apl WHERE apl.user.id = :userId AND apl.photo.id IN :photoIds")
    Set<Long> findLikedPhotoIds(@Param("userId") Long userId, @Param("photoIds") List<Long> photoIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId AND apl.user.id = :userId")
    int deleteByPhotoIdAndUserId(@Param("photoId") Long photoId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistGalleryPhotoLike apl WHERE apl.photo.id = :photoId")
    void deleteByPhotoId(@Param("photoId") Long photoId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE artist_gallery_photo SET like_count = GREATEST(like_count - 1, 0) WHERE id IN (SELECT photo_id FROM artist_gallery_photo_like WHERE user_id = :userId)", nativeQuery = true)
    void decrementLikeCountByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistGalleryPhotoLike apl WHERE apl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 아티스트 삭제 시 해당 아티스트의 모든 사진 좋아요 일괄 삭제 (artist_photos FK 선행 정리)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM artist_photo_likes WHERE artist_photo_id IN (SELECT id FROM artist_photos WHERE artist_id = :artistId)", nativeQuery = true)
    void deleteByArtistId(@Param("artistId") Long artistId);
}
