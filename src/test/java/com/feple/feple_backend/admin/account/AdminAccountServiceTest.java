package com.feple.feple_backend.admin.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.feple.feple_backend.file.service.FileStorageService;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock AdminAccountRepository accountRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FileStorageService fileStorageService;
    @Mock SessionRegistry sessionRegistry;

    @InjectMocks AdminAccountService service;

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private static AdminAccountCreateRequestDto createReq(String username, String password, AdminRole role) {
        return new AdminAccountCreateRequestDto(username, password, "표시이름", role,
                Set.of(), Set.of(AdminPermission.POSTS), null);
    }

    private static AdminAccount buildAccount(Long id, String username, AdminRole role, boolean enabled) {
        return AdminAccount.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .displayName("표시이름")
                .role(role)
                .enabled(enabled)
                .build();
    }

    private static AdminAccount buildAccountWithProfileImage(Long id, String username, String profileImageUrl) {
        return AdminAccount.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .displayName("표시이름")
                .role(AdminRole.MANAGER)
                .enabled(true)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    private void stubFindById(Long id, AdminAccount account) {
        given(accountRepository.findById(id)).willReturn(Optional.of(account));
    }

    // ── create: 정상 흐름 ────────────────────────────────────────────────────

    @Test
    void MANAGER_계정_생성_성공() throws IOException {
        AdminAccount saved = buildAccount(1L, "admin1", AdminRole.MANAGER, true);
        given(accountRepository.existsByUsername("admin1")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(accountRepository.save(any())).willReturn(saved);

        AdminAccount result = service.create(createReq("admin1", "Pass1234!", AdminRole.MANAGER));

        verify(accountRepository).save(any());
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void 계정_생성_동시요청으로_유니크_제약_위반시_IllegalArgumentException으로_변환() {
        given(accountRepository.existsByUsername("admin1")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(accountRepository.save(any()))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.create(createReq("admin1", "Pass1234!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 아이디입니다");
    }

    @Test
    void SUPER_ADMIN_생성시_권한목록은_항상_비어있음() throws IOException {
        AdminAccount saved = buildAccount(2L, "super1", AdminRole.SUPER_ADMIN, true);
        given(accountRepository.existsByUsername("super1")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(accountRepository.save(any())).willReturn(saved);

        service.create(createReq("super1", "Pass1234!", AdminRole.SUPER_ADMIN));

        ArgumentCaptor<AdminAccount> captor = ArgumentCaptor.forClass(AdminAccount.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions()).isEmpty();
    }

    @Test
    void MANAGER_생성시_읽기_쓰기_권한_병합해_저장() throws IOException {
        AdminAccountCreateRequestDto req = new AdminAccountCreateRequestDto(
                "manager1", "Pass1234!", "표시이름", AdminRole.MANAGER,
                Set.of(AdminPermission.POSTS), Set.of(AdminPermission.USERS), null);
        AdminAccount saved = buildAccount(3L, "manager1", AdminRole.MANAGER, true);
        given(accountRepository.existsByUsername("manager1")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(accountRepository.save(any())).willReturn(saved);

        service.create(req);

        ArgumentCaptor<AdminAccount> captor = ArgumentCaptor.forClass(AdminAccount.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions())
                .containsOnly(
                        entry(AdminPermission.POSTS, AdminPermissionLevel.READ),
                        entry(AdminPermission.USERS, AdminPermissionLevel.WRITE));
    }

    @Test
    void MANAGER_생성시_같은_항목에_읽기_쓰기_모두_체크되면_WRITE로_승격() throws IOException {
        AdminAccountCreateRequestDto req = new AdminAccountCreateRequestDto(
                "manager2", "Pass1234!", "표시이름", AdminRole.MANAGER,
                Set.of(AdminPermission.POSTS), Set.of(AdminPermission.POSTS), null);
        AdminAccount saved = buildAccount(4L, "manager2", AdminRole.MANAGER, true);
        given(accountRepository.existsByUsername("manager2")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(accountRepository.save(any())).willReturn(saved);

        service.create(req);

        ArgumentCaptor<AdminAccount> captor = ArgumentCaptor.forClass(AdminAccount.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissions())
                .containsExactly(entry(AdminPermission.POSTS, AdminPermissionLevel.WRITE));
    }

    @Test
    void MANAGER_권한이_하나도_없으면_생성_거부() {
        given(accountRepository.existsByUsername("manager3")).willReturn(false);
        AdminAccountCreateRequestDto req = new AdminAccountCreateRequestDto(
                "manager3", "Pass1234!", "표시이름", AdminRole.MANAGER, Set.of(), Set.of(), null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("일반 관리자는 최소 1개 이상의 접근 권한이 필요합니다.");
        verify(accountRepository, never()).save(any());
    }

    // ── create: 아이디 검증 ──────────────────────────────────────────────────

    @Test
    void 아이디_공백이면_생성_거부() {
        assertThatThrownBy(() -> service.create(createReq("  ", "Pass1234!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디를 입력해주세요.");
    }

    @Test
    void 아이디_51자면_생성_거부() {
        assertThatThrownBy(() -> service.create(createReq("a".repeat(51), "Pass1234!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디는 50자 이하여야 합니다.");
    }

    @Test
    void 중복_아이디는_생성_거부() {
        given(accountRepository.existsByUsername("dup")).willReturn(true);

        assertThatThrownBy(() -> service.create(createReq("dup", "Pass1234!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 아이디입니다");
    }

    // ── create: 비밀번호 복잡도 검증 ─────────────────────────────────────────

    @Test
    void 비밀번호_7자_이하면_거부() {
        assertThatThrownBy(() -> service.create(createReq("admin1", "Ab1!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 8자 이상이어야 합니다.");
    }

    @Test
    void 비밀번호_101자면_거부() {
        assertThatThrownBy(() -> service.create(createReq("admin1", "A1!" + "a".repeat(98), AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 100자 이하여야 합니다.");
    }

    @Test
    void 비밀번호에_영문자_없으면_거부() {
        assertThatThrownBy(() -> service.create(createReq("admin1", "12345678!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    @Test
    void 비밀번호에_숫자_없으면_거부() {
        assertThatThrownBy(() -> service.create(createReq("admin1", "Password!", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    @Test
    void 비밀번호에_특수문자_없으면_거부() {
        assertThatThrownBy(() -> service.create(createReq("admin1", "Password1", AdminRole.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void 존재하지_않는_ID_조회시_예외() {
        given(accountRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void 자신의_계정은_삭제_불가() {
        stubFindById(1L, buildAccount(1L, "admin1", AdminRole.MANAGER, true));

        assertThatThrownBy(() -> service.delete(1L, "admin1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 계정은 삭제할 수 없습니다.");
    }

    @Test
    void 마지막_SUPER_ADMIN은_삭제_불가() {
        AdminAccount account = buildAccount(1L, "super1", AdminRole.SUPER_ADMIN, true);
        stubFindById(1L, account);
        given(accountRepository.findByRoleForUpdate(AdminRole.SUPER_ADMIN)).willReturn(List.of(account));

        assertThatThrownBy(() -> service.delete(1L, "other"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("마지막 최고 관리자 계정은 삭제할 수 없습니다.");
    }

    @Test
    void SUPER_ADMIN이_2명이면_삭제_가능() {
        AdminAccount account = buildAccount(1L, "super1", AdminRole.SUPER_ADMIN, true);
        AdminAccount other = buildAccount(2L, "super2", AdminRole.SUPER_ADMIN, true);
        stubFindById(1L, account);
        given(accountRepository.findByRoleForUpdate(AdminRole.SUPER_ADMIN)).willReturn(List.of(account, other));

        service.delete(1L, "other");

        verify(accountRepository).delete(account);
    }

    @Test
    void MANAGER_계정_삭제_성공() {
        AdminAccount account = buildAccount(1L, "manager1", AdminRole.MANAGER, true);
        stubFindById(1L, account);

        AdminAccount result = service.delete(1L, "admin");

        verify(accountRepository).delete(account);
        assertThat(result).isEqualTo(account);
    }

    // ── toggleEnabled ─────────────────────────────────────────────────────────

    @Test
    void 활성화된_자신의_계정은_비활성화_불가() {
        stubFindById(1L, buildAccount(1L, "admin1", AdminRole.MANAGER, true));

        assertThatThrownBy(() -> service.toggleEnabled(1L, "admin1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 계정을 비활성화할 수 없습니다.");
    }

    @Test
    void 비활성화된_자신의_계정은_재활성화_가능() {
        AdminAccount account = buildAccount(1L, "admin1", AdminRole.MANAGER, false);
        stubFindById(1L, account);

        service.toggleEnabled(1L, "admin1");

        assertThat(account.isEnabled()).isTrue();
    }

    @Test
    void 마지막_활성_SUPER_ADMIN은_비활성화_불가() {
        AdminAccount account = buildAccount(1L, "super1", AdminRole.SUPER_ADMIN, true);
        stubFindById(1L, account);
        given(accountRepository.findByRoleAndEnabledForUpdate(AdminRole.SUPER_ADMIN, true)).willReturn(List.of(account));

        assertThatThrownBy(() -> service.toggleEnabled(1L, "other"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("마지막 활성 최고 관리자 계정은 비활성화할 수 없습니다.");
    }

    @Test
    void 활성_SUPER_ADMIN이_2명이면_비활성화_가능() {
        AdminAccount account = buildAccount(1L, "super1", AdminRole.SUPER_ADMIN, true);
        AdminAccount other = buildAccount(2L, "super2", AdminRole.SUPER_ADMIN, true);
        stubFindById(1L, account);
        given(accountRepository.findByRoleAndEnabledForUpdate(AdminRole.SUPER_ADMIN, true)).willReturn(List.of(account, other));

        service.toggleEnabled(1L, "other");

        assertThat(account.isEnabled()).isFalse();
    }

    @Test
    void MANAGER_토글시_활성에서_비활성으로_전환() {
        AdminAccount account = buildAccount(1L, "manager1", AdminRole.MANAGER, true);
        stubFindById(1L, account);

        service.toggleEnabled(1L, "other");

        assertThat(account.isEnabled()).isFalse();
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void 마지막_SUPER_ADMIN의_역할은_변경_불가() throws IOException {
        AdminAccount account = buildAccount(1L, "super1", AdminRole.SUPER_ADMIN, true);
        stubFindById(1L, account);
        given(accountRepository.findByRoleForUpdate(AdminRole.SUPER_ADMIN)).willReturn(List.of(account));

        AdminAccountUpdateRequestDto req = new AdminAccountUpdateRequestDto(
                "새이름", AdminRole.MANAGER, Set.of(AdminPermission.POSTS), Set.of(), null, null, false);

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("마지막 최고 관리자의 역할을 변경할 수 없습니다.");
    }

    @Test
    void 새_비밀번호_입력시_인코딩되어_저장() throws IOException {
        AdminAccount account = buildAccount(1L, "admin1", AdminRole.MANAGER, true);
        stubFindById(1L, account);
        given(passwordEncoder.encode("NewPass1!")).willReturn("newEncoded");

        AdminAccountUpdateRequestDto req = new AdminAccountUpdateRequestDto(
                "새이름", AdminRole.MANAGER, Set.of(AdminPermission.POSTS), Set.of(), "NewPass1!", null, false);

        service.update(1L, req);

        assertThat(account.getPassword()).isEqualTo("newEncoded");
    }

    @Test
    void 새_비밀번호가_null이면_비밀번호_변경_없음() throws IOException {
        AdminAccount account = buildAccount(1L, "admin1", AdminRole.MANAGER, true);
        stubFindById(1L, account);

        AdminAccountUpdateRequestDto req = new AdminAccountUpdateRequestDto(
                "새이름", AdminRole.MANAGER, Set.of(AdminPermission.POSTS), Set.of(), null, null, false);

        service.update(1L, req);

        assertThat(account.getPassword()).isEqualTo("encoded");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void 프로필_이미지_삭제_요청시_null로_변경() throws IOException {
        AdminAccount account = buildAccount(1L, "admin1", AdminRole.MANAGER, true);
        stubFindById(1L, account);

        AdminAccountUpdateRequestDto req = new AdminAccountUpdateRequestDto(
                "새이름", AdminRole.MANAGER, Set.of(AdminPermission.POSTS), Set.of(), null, null, true);

        service.update(1L, req);

        assertThat(account.getProfileImageUrl()).isNull();
    }

    @Test
    void 프로필_이미지_삭제_요청시_기존_파일_스토리지에서_제거() throws IOException {
        AdminAccount account = buildAccountWithProfileImage(1L, "admin1", "https://cdn.example.com/admin-profiles/old.jpg");
        stubFindById(1L, account);

        AdminAccountUpdateRequestDto req = new AdminAccountUpdateRequestDto(
                "새이름", AdminRole.MANAGER, Set.of(AdminPermission.POSTS), Set.of(), null, null, true);

        service.update(1L, req);

        verify(fileStorageService).deleteFileAfterCommit("https://cdn.example.com/admin-profiles/old.jpg");
    }

    @Test
    void 계정_삭제시_프로필_이미지도_스토리지에서_제거() {
        AdminAccount account = buildAccountWithProfileImage(1L, "manager1", "https://cdn.example.com/admin-profiles/old.jpg");
        stubFindById(1L, account);

        service.delete(1L, "admin");

        verify(fileStorageService).deleteFileAfterCommit("https://cdn.example.com/admin-profiles/old.jpg");
    }
}
