package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.UserPointLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPointLogRepository extends JpaRepository<UserPointLog, Long> {}
