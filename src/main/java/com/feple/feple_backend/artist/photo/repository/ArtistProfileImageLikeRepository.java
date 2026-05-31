package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImageLike;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtistProfileImageLikeRepository extends JpaRepository<ArtistProfileImageLike, Long> {
    Optional<ArtistProfileImageLike> findByUserAndArtistProfileImage(User user, ArtistProfileImage artistProfileImage);

    @Modifying
    @Query("DELETE FROM ArtistProfileImageLike ail WHERE ail.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
