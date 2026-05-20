package com.feple.feple_backend.artist.song.repository;

import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistFestivalSongRepository extends JpaRepository<ArtistFestivalSong, Long> {

    @Query("SELECT afs FROM ArtistFestivalSong afs WHERE afs.artistFestival.id = :artistFestivalId")
    List<ArtistFestivalSong> findByArtistFestivalId(@Param("artistFestivalId") Long artistFestivalId);

    @Query("SELECT afs.song.id, COUNT(afs) FROM ArtistFestivalSong afs WHERE afs.song.artist.id = :artistId GROUP BY afs.song.id")
    List<Object[]> countGroupedBySongForArtist(@Param("artistId") Long artistId);

    @Query("SELECT afs FROM ArtistFestivalSong afs JOIN FETCH afs.artistFestival af JOIN FETCH af.festival WHERE afs.song.id = :songId")
    List<ArtistFestivalSong> findBySongIdWithFestival(@Param("songId") Long songId);

    @Modifying
    @Query("DELETE FROM ArtistFestivalSong afs WHERE afs.artistFestival.id = :artistFestivalId")
    void deleteByArtistFestivalId(@Param("artistFestivalId") Long artistFestivalId);
}
