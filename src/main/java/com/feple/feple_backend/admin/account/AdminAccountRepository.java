package com.feple.feple_backend.admin.account;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(AdminRole role);

    // "마지막 SUPER_ADMIN 보호" 체크 전용 — 동시 요청(두 관리자가 서로 다른 SUPER_ADMIN 계정을
    // 동시에 삭제/비활성화/강등)이 카운트 체크를 동시에 통과해 SUPER_ADMIN이 0명이 되는 TOCTOU
    // 레이스를 막기 위해 대상 행을 잠그고 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminAccount a WHERE a.role = :role")
    List<AdminAccount> findByRoleForUpdate(@Param("role") AdminRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminAccount a WHERE a.role = :role AND a.enabled = :enabled")
    List<AdminAccount> findByRoleAndEnabledForUpdate(@Param("role") AdminRole role, @Param("enabled") boolean enabled);
}
