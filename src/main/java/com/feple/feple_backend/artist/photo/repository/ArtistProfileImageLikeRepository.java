package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImageLike;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistProfileImageLikeRepository extends JpaRepository<ArtistProfileImageLike, Long> {
    Optional<ArtistProfileImageLike> findByUserAndArtistProfileImage(User user, ArtistProfileImage artistProfileImage);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistProfileImageLike ail WHERE ail.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistProfileImageLike ail WHERE ail.artistProfileImage.id IN :imageIds")
    void deleteByArtistProfileImageIdIn(@Param("imageIds") List<Long> imageIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistProfileImageLike ail WHERE ail.user.id = :userId AND ail.artistProfileImage.id = :imageId")
    int deleteByUserIdAndImageId(@Param("userId") Long userId, @Param("imageId") Long imageId);
}
