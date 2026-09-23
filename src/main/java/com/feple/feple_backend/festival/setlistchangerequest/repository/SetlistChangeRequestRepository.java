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

    // keyword는 호출부에서 JpqlLikeEscaper.escape를 거친 값 — %, _ 를 리터럴로 취급하려면
    // ESCAPE '!' 가 함께 있어야 한다(코드베이스의 다른 검색 쿼리와 동일 조합).
    @Query("SELECT r FROM SetlistChangeRequest r WHERE (:status IS NULL OR r.status = :status) "
            + "AND (LOWER(r.artistName) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!' "
            + "OR LOWER(r.festivalTitle) LIKE LOWER(CONCAT('%',:keyword,'%')) ESCAPE '!') "
            + "ORDER BY r.createdAt DESC")
    Page<SetlistChangeRequest> findByStatusAndKeyword(@Param("status") SetlistChangeRequestStatus status, @Param("keyword") String keyword, Pageable pageable);

    long countByStatus(SetlistChangeRequestStatus status);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM SetlistChangeRequest r " +
           "WHERE r.user.id = :userId AND r.artistFestivalId = :artistFestivalId AND r.status = :status")
    boolean existsByUserIdAndArtistFestivalIdAndStatus(@Param("userId") Long userId,
            @Param("artistFestivalId") Long artistFestivalId, @Param("status") SetlistChangeRequestStatus status);
}
