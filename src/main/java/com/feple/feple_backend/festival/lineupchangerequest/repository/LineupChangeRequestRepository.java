package com.feple.feple_backend.festival.lineupchangerequest.repository;

import com.feple.feple_backend.festival.lineupchangerequest.entity.LineupChangeRequest;
import com.feple.feple_backend.festival.lineupchangerequest.entity.LineupChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineupChangeRequestRepository extends JpaRepository<LineupChangeRequest, Long> {

    @Query("SELECT r FROM LineupChangeRequest r WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    Page<LineupChangeRequest> findByStatus(@Param("status") LineupChangeRequestStatus status, Pageable pageable);

    @Query("SELECT r FROM LineupChangeRequest r WHERE (:status IS NULL OR r.status = :status) AND (LOWER(r.artistName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(r.festivalTitle) LIKE LOWER(CONCAT('%',:keyword,'%'))) ORDER BY r.createdAt DESC")
    Page<LineupChangeRequest> findByStatusAndKeyword(@Param("status") LineupChangeRequestStatus status, @Param("keyword") String keyword, Pageable pageable);

    long countByStatus(LineupChangeRequestStatus status);
}
