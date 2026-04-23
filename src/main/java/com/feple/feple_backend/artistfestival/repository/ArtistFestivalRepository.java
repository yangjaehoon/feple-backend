package com.feple.feple_backend.artistfestival.repository;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistFestivalRepository extends JpaRepository<ArtistFestival, Long> {

    List<ArtistFestival> findByFestivalId(Long festivalId);
    List<ArtistFestival> findByFestivalIdOrderByLineupOrderAsc(Long festivalId);
    List<ArtistFestival> findByArtistIdOrderByFestivalStartDateAsc(Long artistId);
    List<ArtistFestival> findByArtistIdOrderByFestivalStartDateDesc(Long artistId);

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.artist WHERE af.festival.id IN :festivalIds ORDER BY af.lineupOrder ASC")
    List<ArtistFestival> findByFestivalIdInWithArtist(@Param("festivalIds") List<Long> festivalIds);

    boolean existsByFestivalIdAndArtistId(Long festivalId, Long artistId);

    long deleteByFestivalId(Long festivalId);

    @Modifying
    @Query("DELETE FROM ArtistFestival af WHERE af.artist.id = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);

}
