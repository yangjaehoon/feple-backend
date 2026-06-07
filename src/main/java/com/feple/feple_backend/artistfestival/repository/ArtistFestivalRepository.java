package com.feple.feple_backend.artistfestival.repository;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistFestivalRepository extends JpaRepository<ArtistFestival, Long> {

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.festival WHERE af.id = :id")
    Optional<ArtistFestival> findByIdWithFestival(@Param("id") Long id);

    @Query("SELECT af FROM ArtistFestival af WHERE af.festival.id = :festivalId")
    List<ArtistFestival> findByFestivalId(@Param("festivalId") Long festivalId);

    // artist JOIN FETCH — getLineup()에서 af.getArtist() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.artist WHERE af.festival.id = :festivalId ORDER BY af.lineupOrder ASC")
    List<ArtistFestival> findByFestivalIdOrderByLineupOrderAsc(@Param("festivalId") Long festivalId);

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.festival WHERE af.artist.id = :artistId ORDER BY af.festival.startDate ASC")
    List<ArtistFestival> findByArtistIdOrderByFestivalStartDateAsc(@Param("artistId") Long artistId);

    // festival JOIN FETCH — getArtistSchedule()에서 af.getFestival() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.festival WHERE af.artist.id = :artistId ORDER BY af.festival.startDate DESC")
    List<ArtistFestival> findByArtistIdOrderByFestivalStartDateDesc(@Param("artistId") Long artistId);

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.artist WHERE af.festival.id IN :festivalIds ORDER BY af.lineupOrder ASC")
    List<ArtistFestival> findByFestivalIdInWithArtist(@Param("festivalIds") List<Long> festivalIds);

    @Query("SELECT CASE WHEN COUNT(af) > 0 THEN TRUE ELSE FALSE END FROM ArtistFestival af WHERE af.festival.id = :festivalId AND af.artist.id = :artistId")
    boolean existsByFestivalIdAndArtistId(@Param("festivalId") Long festivalId, @Param("artistId") Long artistId);

    @Modifying
    @Query("DELETE FROM ArtistFestival af WHERE af.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying
    @Query("DELETE FROM ArtistFestival af WHERE af.artist.id = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);
}
