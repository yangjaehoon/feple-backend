package com.feple.feple_backend.appconfig.repository;

import com.feple.feple_backend.appconfig.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}
