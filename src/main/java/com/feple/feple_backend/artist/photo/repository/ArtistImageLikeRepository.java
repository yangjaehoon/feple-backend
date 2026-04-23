package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistImage;
import com.feple.feple_backend.artist.photo.entity.ArtistImageLike;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtistImageLikeRepository extends JpaRepository<ArtistImageLike, Long> {
    Optional<ArtistImageLike> findByUserAndArtistImage(User user, ArtistImage artistImage);

    @Modifying
    @Query("DELETE FROM ArtistImageLike ail WHERE ail.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
