package com.feple.feple_backend.repository;

import com.feple.feple_backend.artist.entity.ArtistImage;
import com.feple.feple_backend.artist.entity.ArtistImageLike;
import com.feple.feple_backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistImageLikeRepository extends JpaRepository<ArtistImageLike, Long> {
    Optional<ArtistImageLike> findByUserAndArtistImage(User user, ArtistImage artistImage);
}
