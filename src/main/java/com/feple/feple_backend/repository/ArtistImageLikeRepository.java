package com.feple.feple_backend.repository;

import com.feple.feple_backend.domain.artist.ArtistImage;
import com.feple.feple_backend.domain.artist.ArtistImageLike;
import com.feple.feple_backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistImageLikeRepository extends JpaRepository<ArtistImageLike, Long> {
    Optional<ArtistImageLike> findByUserAndArtistImage(User user, ArtistImage artistImage);
}
