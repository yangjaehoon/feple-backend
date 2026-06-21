package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistGalleryPhotoRepository extends JpaRepository<ArtistGalleryPhoto, Long> {
    @Query("SELECT p FROM ArtistGalleryPhoto p WHERE p.artist.id = :artistId ORDER BY p.id DESC")
    List<ArtistGalleryPhoto> findByArtist_IdOrderByIdDesc(@Param("artistId") Long artistId);

    @Query("SELECT p FROM ArtistGalleryPhoto p WHERE p.artist.id = :artistId ORDER BY p.likeCount DESC, p.createdAt DESC")
    List<ArtistGalleryPhoto> findByArtist_IdOrderByLikeCountDescCreatedAtDesc(@Param("artistId") Long artistId);

    @Query("SELECT p FROM ArtistGalleryPhoto p WHERE p.id = :id AND p.artist.id = :artistId")
    ArtistGalleryPhoto findByIdAndArtist_Id(@Param("id") Long id, @Param("artistId") Long artistId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ArtistGalleryPhoto p SET p.likeCount = p.likeCount + 1 WHERE p.id = :photoId")
    void incrementLikeCount(@Param("photoId") Long photoId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ArtistGalleryPhoto p SET p.likeCount = p.likeCount - 1 WHERE p.id = :photoId AND p.likeCount > 0")
    void decrementLikeCount(@Param("photoId") Long photoId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistGalleryPhoto p WHERE p.artist.id = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);
}
