package com.feple.feple_backend.admin.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(AdminRole role);

    long countByRole(AdminRole role);

    @Query("SELECT COUNT(a) FROM AdminAccount a WHERE a.role = :role AND a.enabled = :enabled")
    long countByRoleAndEnabled(@Param("role") AdminRole role, @Param("enabled") boolean enabled);
}
