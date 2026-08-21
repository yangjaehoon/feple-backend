package com.feple.feple_backend.ticketlink.repository;

import com.feple.feple_backend.ticketlink.entity.FestivalTicketLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalTicketLinkRepository extends JpaRepository<FestivalTicketLink, Long> {
    @Query("SELECT t FROM FestivalTicketLink t WHERE t.festival.id = :festivalId ORDER BY t.id")
    List<FestivalTicketLink> findByFestivalId(@Param("festivalId") Long festivalId);
}
