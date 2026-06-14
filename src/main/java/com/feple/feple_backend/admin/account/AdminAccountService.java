package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
    private final FileStorageService fileStorageService;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public List<AdminAccount> findAll() {
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Optional<AdminAccount> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public AdminAccount findById(Long id) {
        return EntityFinder.getOrThrow(accountRepository::findById, id, "관리자 계정");
    }

    public void create(AdminAccountCreateRequest req) throws IOException {
        validateNewAccount(req.username(), req.password());
        AdminAccount account = accountRepository.save(AdminAccount.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .role(req.role())
                .permissions(resolvePermissions(req.role(), req.permissions()))
                .profileImageUrl(uploadProfileIfPresent(req.profileImage(), req.username()))
                .build());
        adminLogService.log(AdminAction.ADMIN_ACCOUNT_CREATE, "ADMIN_ACCOUNT", account.getId(), req.username());
    }

    public void update(Long id, AdminAccountUpdateRequest req) throws IOException {
        AdminAccount account = findById(id);
        validateRoleChange(account, req.role());
        account.updateProfile(req.displayName(), req.role(), resolvePermissions(req.role(), req.permissions()));
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            validatePasswordComplexity(req.newPassword());
            account.updatePassword(passwordEncoder.encode(req.newPassword()));
        }
        applyProfileImageUpdate(account, req);
        adminLogService.log(AdminAction.ADMIN_ACCOUNT_UPDATE, "ADMIN_ACCOUNT", id, account.getUsername());
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

        adminLogService.log(AdminAction.ADMIN_ACCOUNT_DELETE, "ADMIN_ACCOUNT", id, account.getUsername());
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

        account.toggle();
        String detail = account.getUsername() + " → " + (account.isEnabled() ? "활성" : "비활성");
        adminLogService.log(AdminAction.ADMIN_ACCOUNT_TOGGLE, "ADMIN_ACCOUNT", id, detail);
    }

    private void validateNewAccount(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        if (username.length() > 50)
            throw new IllegalArgumentException("아이디는 50자 이하여야 합니다.");
        validatePasswordComplexity(password);
        if (accountRepository.existsByUsername(username))
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + username);
    }

    private static void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        if (password.length() > 100)
            throw new IllegalArgumentException("비밀번호는 100자 이하여야 합니다.");
        boolean hasLetter  = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!hasLetter || !hasDigit || !hasSpecial)
            throw new IllegalArgumentException("비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    private String uploadProfileIfPresent(org.springframework.web.multipart.MultipartFile profileImage,
                                          String username) throws IOException {
        return (profileImage != null && !profileImage.isEmpty())
                ? fileStorageService.storeAdminProfile(profileImage, username)
                : null;
    }

    private void validateRoleChange(AdminAccount account, AdminRole newRole) {
        if (account.getRole() == AdminRole.SUPER_ADMIN
                && newRole == AdminRole.MANAGER
                && accountRepository.countByRole(AdminRole.SUPER_ADMIN) <= 1) {
            throw new IllegalArgumentException("마지막 최고 관리자의 역할을 변경할 수 없습니다.");
        }
    }

    private void applyProfileImageUpdate(AdminAccount account, AdminAccountUpdateRequest req) throws IOException {
        if (req.deleteProfileImage()) {
            account.updateProfileImage(null);
        } else if (req.profileImage() != null && !req.profileImage().isEmpty()) {
            account.updateProfileImage(
                    fileStorageService.storeAdminProfile(req.profileImage(), account.getUsername()));
        }
    }

    private static Set<AdminPermission> resolvePermissions(AdminRole role, Set<AdminPermission> permissions) {
        return role == AdminRole.SUPER_ADMIN ? new HashSet<>() : new HashSet<>(permissions);
    }
}
