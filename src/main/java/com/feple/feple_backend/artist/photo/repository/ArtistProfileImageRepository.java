package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistProfileImageRepository extends JpaRepository<ArtistProfileImage, Long> {
    List<ArtistProfileImage> findByArtist(Artist artist);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ArtistProfileImage ai SET ai.uploader = null WHERE ai.uploader.id = :userId")
    void nullifyUploaderByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ArtistProfileImage ai SET ai.likeCount = ai.likeCount + 1 WHERE ai.id = :imageId")
    void incrementLikeCount(@Param("imageId") Long imageId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ArtistProfileImage ai SET ai.likeCount = ai.likeCount - 1 WHERE ai.id = :imageId AND ai.likeCount > 0")
    void decrementLikeCount(@Param("imageId") Long imageId);
}
