package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistImageRepository extends JpaRepository<ArtistImage, Long> {
    List<ArtistImage> findByArtist(Artist artist);

    @Modifying
    @Query("UPDATE ArtistImage ai SET ai.uploader = null WHERE ai.uploader.id = :userId")
    void nullifyUploaderByUserId(@Param("userId") Long userId);
}

