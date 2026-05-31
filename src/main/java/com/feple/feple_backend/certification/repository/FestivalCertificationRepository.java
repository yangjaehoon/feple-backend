package com.feple.feple_backend.certification.repository;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FestivalCertificationRepository extends JpaRepository<FestivalCertification, Long> {

    @Query("SELECT fc FROM FestivalCertification fc WHERE fc.user.id = :userId AND fc.festival.id = :festivalId")
    Optional<FestivalCertification> findByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    @Query("SELECT fc FROM FestivalCertification fc WHERE fc.user.id = :userId")
    List<FestivalCertification> findByUserId(@Param("userId") Long userId);

    @Query("SELECT fc FROM FestivalCertification fc WHERE fc.user.id = :userId AND fc.status = :status")
    List<FestivalCertification> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CertificationStatus status);

    Page<FestivalCertification> findByStatus(CertificationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "festival"})
    Page<FestivalCertification> findByStatusOrderByCreatedAtDesc(CertificationStatus status, Pageable pageable);

    long countByStatus(CertificationStatus status);

    Page<FestivalCertification> findAll(Pageable pageable);

    /** 특정 페스티벌의 승인된 인증 유저 ID 목록 (게시글/댓글 뱃지용) */
    @Query("SELECT fc.user.id FROM FestivalCertification fc WHERE fc.festival.id = :festivalId AND fc.status = 'APPROVED'")
    Set<Long> findApprovedUserIdsByFestivalId(@Param("festivalId") Long festivalId);

    @Query("SELECT c FROM FestivalCertification c JOIN c.user u JOIN c.festival f " +
           "WHERE (:status IS NULL OR c.status = :status) " +
           "AND (LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<FestivalCertification> searchByKeyword(@Param("keyword") String keyword,
                                                @Param("status") CertificationStatus status,
                                                Pageable pageable);

    @Modifying
    @Query("DELETE FROM FestivalCertification fc WHERE fc.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM FestivalCertification fc WHERE fc.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
