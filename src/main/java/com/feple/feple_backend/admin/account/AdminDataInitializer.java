package com.feple.feple_backend.admin.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (adminAccountRepository.count() > 0) {
            return;
        }

        if (adminPassword.isBlank()) {
            log.warn("초기 관리자 비밀번호(app.admin.password)가 설정되지 않아 초기 계정을 생성하지 않습니다.");
            return;
        }

        AdminAccount superAdmin = AdminAccount.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .displayName("최고 관리자")
                .role(AdminRole.SUPER_ADMIN)
                .build();

        adminAccountRepository.save(superAdmin);
        log.info("초기 SUPER_ADMIN 계정 생성: {}", adminUsername);
    }
}
