package com.feple.feple_backend.certification.repository;

import com.feple.feple_backend.certification.entity.CertificationReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

public interface CertificationReviewLikeRepository extends JpaRepository<CertificationReviewLike, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM CertificationReviewLike rl WHERE rl.userId = :userId AND rl.certificationId = :certId")
    int deleteByUserIdAndCertificationId(@Param("userId") Long userId, @Param("certId") Long certId);

    @Query("SELECT rl.certificationId FROM CertificationReviewLike rl WHERE rl.userId = :userId AND rl.certificationId IN :certIds")
    Set<Long> findLikedCertIdsByUserIdIn(@Param("userId") Long userId, @Param("certIds") Set<Long> certIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM CertificationReviewLike rl WHERE rl.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CertificationReviewLike rl WHERE rl.certificationId IN (SELECT fc.id FROM FestivalCertification fc WHERE fc.user.id = :userId)")
    void deleteByCertificationUserId(@Param("userId") Long userId);
}
