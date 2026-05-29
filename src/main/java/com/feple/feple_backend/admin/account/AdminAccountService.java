package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.global.EntityFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAccountService {

    private final AdminAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminAccount> findAll() {
        return accountRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Optional<AdminAccount> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public AdminAccount findById(Long id) {
        return EntityFinder.getOrThrow(accountRepository::findById, id, "관리자 계정");
    }

    public void create(String username, String password, String displayName,
                       AdminRole role, Set<AdminPermission> permissions) {
        if (accountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + username);
        }

        Set<AdminPermission> effectivePerms = (role == AdminRole.SUPER_ADMIN)
                ? new HashSet<>()
                : new HashSet<>(permissions);

        AdminAccount account = AdminAccount.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .displayName(displayName)
                .role(role)
                .permissions(effectivePerms)
                .build();

        accountRepository.save(account);
    }

    public void update(Long id, String displayName, AdminRole role,
                       Set<AdminPermission> permissions, String newPassword) {
        AdminAccount account = findById(id);

        if (account.getRole() == AdminRole.SUPER_ADMIN
                && role == AdminRole.MANAGER
                && accountRepository.countByRole(AdminRole.SUPER_ADMIN) <= 1) {
            throw new IllegalArgumentException("마지막 최고 관리자의 역할을 변경할 수 없습니다.");
        }

        Set<AdminPermission> effectivePerms = (role == AdminRole.SUPER_ADMIN)
                ? new HashSet<>()
                : new HashSet<>(permissions);

        account.updateProfile(displayName, role, effectivePerms);

        if (newPassword != null && !newPassword.isBlank()) {
            account.updatePassword(passwordEncoder.encode(newPassword));
        }
    }

    public void delete(Long id, String currentUsername) {
        AdminAccount account = findById(id);

        if (account.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("자신의 계정은 삭제할 수 없습니다.");
        }

        if (account.getRole() == AdminRole.SUPER_ADMIN
                && accountRepository.countByRole(AdminRole.SUPER_ADMIN) <= 1) {
            throw new IllegalArgumentException("마지막 최고 관리자 계정은 삭제할 수 없습니다.");
        }

        accountRepository.delete(account);
    }

    public void toggleEnabled(Long id, String currentUsername) {
        AdminAccount account = findById(id);

        if (account.getUsername().equals(currentUsername) && account.isEnabled()) {
            throw new IllegalArgumentException("자신의 계정을 비활성화할 수 없습니다.");
        }

        if (account.isEnabled()
                && account.getRole() == AdminRole.SUPER_ADMIN
                && accountRepository.countByRoleAndEnabled(AdminRole.SUPER_ADMIN, true) <= 1) {
            throw new IllegalArgumentException("마지막 활성 최고 관리자 계정은 비활성화할 수 없습니다.");
        }

        if (account.isEnabled()) {
            account.disable();
        } else {
            account.enable();
        }
    }
}
