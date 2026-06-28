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
import org.springframework.transaction.annotation.Transactional;

public interface FestivalCertificationRepository extends JpaRepository<FestivalCertification, Long> {

    @Query("SELECT fc FROM FestivalCertification fc WHERE fc.user.id = :userId AND fc.festival.id = :festivalId")
    Optional<FestivalCertification> findByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    @Query("SELECT fc FROM FestivalCertification fc JOIN FETCH fc.festival WHERE fc.user.id = :userId")
    List<FestivalCertification> findByUserId(@Param("userId") Long userId);

    @Query("SELECT fc FROM FestivalCertification fc WHERE fc.user.id = :userId AND fc.status = :status")
    List<FestivalCertification> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CertificationStatus status);

    @EntityGraph(attributePaths = {"user", "festival"})
    Page<FestivalCertification> findByStatus(CertificationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "festival"})
    Page<FestivalCertification> findByStatusOrderByCreatedAtDesc(CertificationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "festival"})
    Optional<FestivalCertification> findWithUserAndFestivalById(Long id);

    long countByStatus(CertificationStatus status);

    @Query("SELECT COUNT(fc) FROM FestivalCertification fc WHERE fc.user.id = :userId AND fc.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CertificationStatus status);

    @EntityGraph(attributePaths = {"user", "festival"})
    Page<FestivalCertification> findAll(Pageable pageable);

    /** 특정 페스티벌의 승인된 인증 유저 ID 목록 (게시글/댓글 뱃지용) */
    @Query("SELECT fc.user.id FROM FestivalCertification fc WHERE fc.festival.id = :festivalId AND fc.status = 'APPROVED'")
    Set<Long> findApprovedUserIdsByFestivalId(@Param("festivalId") Long festivalId);

    /** 댓글 작성 시 작성자 인증 여부 단건 확인 — 전체 Set 로드 방지 */
    @Query("SELECT CASE WHEN COUNT(fc) > 0 THEN TRUE ELSE FALSE END FROM FestivalCertification fc " +
           "WHERE fc.festival.id = :festivalId AND fc.user.id = :userId AND fc.status = 'APPROVED'")
    boolean existsApprovedCertification(@Param("festivalId") Long festivalId, @Param("userId") Long userId);

    @Query(value = "SELECT c FROM FestivalCertification c JOIN FETCH c.user u JOIN FETCH c.festival f " +
                   "WHERE (:status IS NULL OR c.status = :status) " +
                   "AND (LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
                   "     OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')",
           countQuery = "SELECT COUNT(c) FROM FestivalCertification c JOIN c.user u JOIN c.festival f " +
                        "WHERE (:status IS NULL OR c.status = :status) " +
                        "AND (LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
                        "     OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')")
    Page<FestivalCertification> searchByKeyword(@Param("keyword") String keyword,
                                                @Param("status") CertificationStatus status,
                                                Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalCertification fc WHERE fc.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(fc.rating) FROM FestivalCertification fc WHERE fc.festival.id = :festivalId AND fc.status = 'APPROVED' AND fc.rating IS NOT NULL")
    Double getAverageRatingByFestivalId(@Param("festivalId") Long festivalId);

    @Query("SELECT COUNT(fc) FROM FestivalCertification fc WHERE fc.festival.id = :festivalId AND fc.status = 'APPROVED' AND fc.rating IS NOT NULL")
    int getRatingCountByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FestivalCertification fc WHERE fc.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
