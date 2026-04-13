package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistPhotoRepository extends JpaRepository<ArtistPhoto, Long> {
    List<ArtistPhoto> findByArtistIdOrderByIdDesc(Long artistId);

    List<ArtistPhoto> findByArtistIdOrderByLikeCountDescCreatedAtDesc(Long artistId);

    ArtistPhoto findByIdAndArtistId(Long id, Long artistId);

    @Modifying
    @Query("UPDATE ArtistPhoto p SET p.likeCount = p.likeCount + 1 WHERE p.id = :photoId")
    void incrementLikeCount(@Param("photoId") Long photoId);

    @Modifying
    @Query("UPDATE ArtistPhoto p SET p.likeCount = p.likeCount - 1 WHERE p.id = :photoId AND p.likeCount > 0")
    void decrementLikeCount(@Param("photoId") Long photoId);
}