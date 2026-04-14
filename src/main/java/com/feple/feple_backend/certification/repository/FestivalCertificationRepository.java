package com.feple.feple_backend.certification.repository;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FestivalCertificationRepository extends JpaRepository<FestivalCertification, Long> {

    Optional<FestivalCertification> findByUserIdAndFestivalId(Long userId, Long festivalId);

    List<FestivalCertification> findByUserId(Long userId);

    List<FestivalCertification> findByUserIdAndStatus(Long userId, CertificationStatus status);

    Page<FestivalCertification> findByStatus(CertificationStatus status, Pageable pageable);

    Page<FestivalCertification> findAll(Pageable pageable);
}
