package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Artist a set a.followerCount = a.followerCount + 1 where a.id = :artistId")
    int incrementFollowerCount(@Param("artistId") Long artistId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
                    update Artist a
                    set a.followerCount = case when a.followerCount > 0 then a.followerCount - 1 else 0 end 
                    where a.id = :artistId
            """
    )
    int decrementFollowerCount(@Param("artistId") Long artistId);

}
