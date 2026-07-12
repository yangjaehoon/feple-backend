package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
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

    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 100;

    private final AdminAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

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
        return EntityLoader.getOrThrow(accountRepository::findById, id, "관리자 계정");
    }

    /** @return 생성된 계정 — 컨트롤러가 감사 로그(id, username) 기록에 사용 */
    public AdminAccount create(AdminAccountCreateRequestDto req) {
        validateNewAccount(req.username(), req.password());
        return accountRepository.save(AdminAccount.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .role(req.role())
                .permissions(resolvePermissions(req.role(), req.permissions()))
                .profileImageUrl(uploadProfileIfPresent(req.profileImage(), req.username()))
                .build());
    }

    public void update(Long id, AdminAccountUpdateRequestDto req) {
        AdminAccount account = findById(id);
        validateRoleChange(account, req.role());
        account.updateProfile(req.displayName(), req.role(), resolvePermissions(req.role(), req.permissions()));
        if (req.password() != null && !req.password().isBlank()) {
            validatePasswordComplexity(req.password());
            account.updatePassword(passwordEncoder.encode(req.password()));
        }
        applyProfileImageUpdate(account, req);
    }

    /** @return 삭제된 계정 — 컨트롤러가 감사 로그(username) 기록에 사용 */
    public AdminAccount delete(Long id, String currentUsername) {
        AdminAccount account = findById(id);

        if (account.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("자신의 계정은 삭제할 수 없습니다.");
        }

        if (account.getRole() == AdminRole.SUPER_ADMIN
                && accountRepository.countByRole(AdminRole.SUPER_ADMIN) <= 1) {
            throw new IllegalArgumentException("마지막 최고 관리자 계정은 삭제할 수 없습니다.");
        }

        accountRepository.delete(account);
        return account;
    }

    /** @return 토글 후 계정 — 컨트롤러가 감사 로그(username, 활성 상태) 기록에 사용 */
    public AdminAccount toggleEnabled(Long id, String currentUsername) {
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
        return account;
    }

    private void validateNewAccount(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        if (username.length() > USERNAME_MAX_LENGTH)
            throw new IllegalArgumentException("아이디는 " + USERNAME_MAX_LENGTH + "자 이하여야 합니다.");
        validatePasswordComplexity(password);
        if (accountRepository.existsByUsername(username))
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + username);
    }

    static void validatePasswordComplexity(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH)
            throw new IllegalArgumentException("비밀번호는 " + PASSWORD_MIN_LENGTH + "자 이상이어야 합니다.");
        if (password.length() > PASSWORD_MAX_LENGTH)
            throw new IllegalArgumentException("비밀번호는 " + PASSWORD_MAX_LENGTH + "자 이하여야 합니다.");
        boolean hasLetter  = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!hasLetter || !hasDigit || !hasSpecial)
            throw new IllegalArgumentException("비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    // IOException을 RuntimeException으로 감싸 서비스 시그니처에서 체크드 예외를 제거한다.
    private String uploadProfileIfPresent(org.springframework.web.multipart.MultipartFile profileImage,
                                          String username) {
        if (profileImage == null || profileImage.isEmpty()) return null;
        try {
            return fileStorageService.storeAdminProfile(profileImage, username);
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지 업로드에 실패했습니다.", e);
        }
    }

    private void validateRoleChange(AdminAccount account, AdminRole newRole) {
        if (account.getRole() == AdminRole.SUPER_ADMIN
                && newRole == AdminRole.MANAGER
                && accountRepository.countByRole(AdminRole.SUPER_ADMIN) <= 1) {
            throw new IllegalArgumentException("마지막 최고 관리자의 역할을 변경할 수 없습니다.");
        }
    }

    private void applyProfileImageUpdate(AdminAccount account, AdminAccountUpdateRequestDto req) {
        if (req.deleteProfileImage()) {
            account.updateProfileImage(null);
        } else if (req.profileImage() != null && !req.profileImage().isEmpty()) {
            try {
                account.updateProfileImage(
                        fileStorageService.storeAdminProfile(req.profileImage(), account.getUsername()));
            } catch (IOException e) {
                throw new IllegalStateException("프로필 이미지 업로드에 실패했습니다.", e);
            }
        }
    }

    private static Set<AdminPermission> resolvePermissions(AdminRole role, Set<AdminPermission> permissions) {
        return role == AdminRole.SUPER_ADMIN ? new HashSet<>() : new HashSet<>(permissions);
    }
}
