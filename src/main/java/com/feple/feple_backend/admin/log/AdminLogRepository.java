package com.feple.feple_backend.admin.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AdminLog> findByTargetTypeOrderByCreatedAtDesc(String targetType, Pageable pageable);
    List<AdminLog> findTop10ByOrderByCreatedAtDesc();
}
