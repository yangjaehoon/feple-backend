package com.feple.feple_backend.festival.setlistchangerequest.repository;

import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequest;
import com.feple.feple_backend.festival.setlistchangerequest.entity.SetlistChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetlistChangeRequestRepository extends JpaRepository<SetlistChangeRequest, Long> {

    @Query("SELECT r FROM SetlistChangeRequest r WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    Page<SetlistChangeRequest> findByStatus(@Param("status") SetlistChangeRequestStatus status, Pageable pageable);

    @Query("SELECT r FROM SetlistChangeRequest r WHERE (:status IS NULL OR r.status = :status) AND (LOWER(r.artistName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(r.festivalTitle) LIKE LOWER(CONCAT('%',:keyword,'%'))) ORDER BY r.createdAt DESC")
    Page<SetlistChangeRequest> findByStatusAndKeyword(@Param("status") SetlistChangeRequestStatus status, @Param("keyword") String keyword, Pageable pageable);

    long countByStatus(SetlistChangeRequestStatus status);
}
