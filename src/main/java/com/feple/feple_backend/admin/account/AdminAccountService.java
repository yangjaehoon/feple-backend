package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SessionRegistry sessionRegistry;

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
        String profileImageUrl = uploadProfileIfPresent(req.profileImage(), req.username());
        // DB 저장 실패로 트랜잭션이 롤백되면 이미 올라간 S3 파일이 orphan으로 남지 않도록 정리
        fileStorageService.deleteFileOnRollback(profileImageUrl);
        // existsByUsername 체크 후 save() 사이의 TOCTOU 레이스(동시 생성)는 유니크 제약이 최종
        // 방어선이다 — AdminActionUtils.tryAction은 ConflictException을 별도 처리하지 않아
        // (Thymeleaf 플로우이지 REST가 아니므로) 위 사전 검증과 동일하게 IllegalArgumentException으로
        // 변환해야 구체적인 에러 메시지가 화면에 그대로 노출된다.
        try {
            return accountRepository.save(AdminAccount.builder()
                    .username(req.username())
                    .password(passwordEncoder.encode(req.password()))
                    .displayName(req.displayName())
                    .role(req.role())
                    .permissions(resolvePermissions(req.role(), req.readPermissions(), req.writePermissions()))
                    .profileImageUrl(profileImageUrl)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + req.username());
        }
    }

    public void update(Long id, AdminAccountUpdateRequestDto req) {
        AdminAccount account = findById(id);
        validateRoleChange(account, req.role());
        account.updateProfile(req.displayName(), req.role(),
                resolvePermissions(req.role(), req.readPermissions(), req.writePermissions()));
        if (req.password() != null && !req.password().isBlank()) {
            validatePasswordComplexity(req.password());
            account.updatePassword(passwordEncoder.encode(req.password()));
        }
        applyProfileImageUpdate(account, req);
        // 역할/권한/비밀번호가 바뀌었을 수 있으므로 로그인 세션에 캐시된 권한이 낡은 상태로 남지
        // 않도록 기존 세션을 만료시킨다 — 다음 요청 시 재로그인하며 최신 권한을 다시 받는다.
        expireSessionsFor(account.getUsername());
    }

    /** @return 삭제된 계정 — 컨트롤러가 감사 로그(username) 기록에 사용 */
    public AdminAccount delete(Long id, String currentUsername) {
        AdminAccount account = findById(id);

        if (account.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("자신의 계정은 삭제할 수 없습니다.");
        }

        if (account.getRole() == AdminRole.SUPER_ADMIN) {
            ensureNotLastSuperAdmin(accountRepository.findByRoleForUpdate(AdminRole.SUPER_ADMIN),
                    "마지막 최고 관리자 계정은 삭제할 수 없습니다.");
        }

        accountRepository.delete(account);
        fileStorageService.deleteFileAfterCommit(account.getProfileImageUrl());
        expireSessionsFor(account.getUsername());
        return account;
    }

    /** @return 토글 후 계정 — 컨트롤러가 감사 로그(username, 활성 상태) 기록에 사용 */
    public AdminAccount toggleEnabled(Long id, String currentUsername) {
        AdminAccount account = findById(id);

        if (account.getUsername().equals(currentUsername) && account.isEnabled()) {
            throw new IllegalArgumentException("자신의 계정을 비활성화할 수 없습니다.");
        }

        if (account.isEnabled() && account.getRole() == AdminRole.SUPER_ADMIN) {
            ensureNotLastSuperAdmin(accountRepository.findByRoleAndEnabledForUpdate(AdminRole.SUPER_ADMIN, true),
                    "마지막 활성 최고 관리자 계정은 비활성화할 수 없습니다.");
        }

        account.toggle();
        if (!account.isEnabled()) {
            expireSessionsFor(account.getUsername());
        }
        return account;
    }

    // 계정 삭제/비활성화/정보변경 시 이미 로그인된 세션에 캐시된 권한이 낡은 채로 유지되지 않도록
    // 강제로 만료시킨다. SessionRegistry는 principal.equals()로 세션을 찾고 User.equals()는
    // username만 비교하므로(계정이 이미 삭제됐을 수도 있어 DB 재조회 없이) 더미 값으로 조회 키를
    // 직접 구성한다.
    private void expireSessionsFor(String username) {
        UserDetails principal = new User(username, "N/A", List.of());
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
            session.expireNow();
        }
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
        if (account.getRole() == AdminRole.SUPER_ADMIN && newRole == AdminRole.MANAGER) {
            ensureNotLastSuperAdmin(accountRepository.findByRoleForUpdate(AdminRole.SUPER_ADMIN),
                    "마지막 최고 관리자의 역할을 변경할 수 없습니다.");
        }
    }

    /** 현재 SUPER_ADMIN 수(currentSuperAdmins, PESSIMISTIC_WRITE로 잠긴 목록)가 1명 이하면 보호 규칙 위반으로 거부한다. */
    private static void ensureNotLastSuperAdmin(List<AdminAccount> currentSuperAdmins, String errorMessage) {
        if (currentSuperAdmins.size() <= 1) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void applyProfileImageUpdate(AdminAccount account, AdminAccountUpdateRequestDto req) {
        if (req.deleteProfileImage()) {
            String oldImageUrl = account.getProfileImageUrl();
            account.updateProfileImage(null);
            fileStorageService.deleteFileAfterCommit(oldImageUrl);
        } else if (req.profileImage() != null && !req.profileImage().isEmpty()) {
            String oldImageUrl = account.getProfileImageUrl();
            String newImageUrl;
            try {
                newImageUrl = fileStorageService.storeAdminProfile(req.profileImage(), account.getUsername());
            } catch (IOException e) {
                throw new IllegalStateException("프로필 이미지 업로드에 실패했습니다.", e);
            }
            // DB 저장 실패로 트랜잭션이 롤백되면 이미 올라간 S3 파일이 orphan으로 남지 않도록 정리
            fileStorageService.deleteFileOnRollback(newImageUrl);
            account.updateProfileImage(newImageUrl);
            fileStorageService.deleteFileAfterCommit(oldImageUrl);
        }
    }

    // SUPER_ADMIN은 권한 맵을 비워둔다(로그인 시 전 권한 동적 부여). MANAGER는 읽기/쓰기 체크박스를
    // 병합하되, 같은 항목에 둘 다 체크됐거나 쓰기만 체크됐으면 WRITE로 승격한다(WRITE ⊇ READ).
    private static Map<AdminPermission, AdminPermissionLevel> resolvePermissions(
            AdminRole role, Set<AdminPermission> readPermissions, Set<AdminPermission> writePermissions) {
        Map<AdminPermission, AdminPermissionLevel> resolved = new EnumMap<>(AdminPermission.class);
        if (role == AdminRole.SUPER_ADMIN) {
            return resolved;
        }
        for (AdminPermission permission : readPermissions) {
            resolved.put(permission, AdminPermissionLevel.READ);
        }
        for (AdminPermission permission : writePermissions) {
            resolved.put(permission, AdminPermissionLevel.WRITE);
        }
        return resolved;
    }
}
