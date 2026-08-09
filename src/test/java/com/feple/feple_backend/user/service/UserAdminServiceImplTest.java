package com.feple.feple_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.CurrentAdminProvider;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Mock UserCascadeDeleteService cascadeDeleteService;
    @Spy CurrentAdminProvider currentAdminProvider = new CurrentAdminProvider();

    @InjectMocks UserAdminServiceImpl userAdminService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(Long id, String nickname) {
        return User.builder().id(id).oauthId("o" + id).nickname(nickname)
                .role(UserRole.USER).build();
    }

    // ── getAdminUser / findByNickname ────────────────────────────────

    @Test
    void 관리자용_사용자_조회() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L, "유저1")));

        UserResponseDto result = userAdminService.getAdminUser(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void 닉네임으로_사용자_조회() {
        given(userRepository.findByNicknameAndNotDeleted("유저1")).willReturn(Optional.of(user(1L, "유저1")));

        UserResponseDto result = userAdminService.findByNickname("유저1");

        assertThat(result.getNickname()).isEqualTo("유저1");
    }

    // ── getUsersPage / getUsersPageSortedByReports / getBannedUsersPage ──

    @Test
    void 키워드_있으면_활성_사용자_키워드_검색() {
        given(userRepository.findActiveByKeyword(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(user(1L, "유저1"))));

        Page<UserResponseDto> result = userAdminService.getUsersPage(0, 20, "유저");

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 키워드_없으면_빈_키워드로_활성_사용자_전체_조회() {
        given(userRepository.findActiveByKeyword(eq(""), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(user(1L, "유저1"))));

        Page<UserResponseDto> result = userAdminService.getUsersPage(0, 20, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 신고순_정렬_사용자_조회() {
        given(userRepository.findAllOrderByTotalReportCountDesc(any(), any()))
                .willReturn(new PageImpl<>(List.of(user(1L, "유저1"))));

        Page<UserResponseDto> result = userAdminService.getUsersPageSortedByReports(0, 20, null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 정지된_사용자_목록_조회() {
        given(userRepository.findBannedUsers(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(user(1L, "정지유저"))));

        Page<UserResponseDto> result = userAdminService.getBannedUsersPage(0, 20, null);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── getTotalCount ──────────────────────────────────────────────────

    @Test
    void 활성_사용자_총_수_조회() {
        given(userRepository.countByDeletedAtIsNull()).willReturn(42L);

        assertThat(userAdminService.getTotalCount()).isEqualTo(42L);
    }

    // ── deleteUser / bulkDeleteUsers ──────────────────────────────────

    @Test
    void 사용자_삭제시_cascadeDeleteService에_위임하고_닉네임_반환() {
        User user = user(1L, "삭제유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        String nickname = userAdminService.adminDeleteUser(1L);

        assertThat(nickname).isEqualTo("삭제유저");
        verify(cascadeDeleteService).delete(user);
    }

    @Test
    void bulkDeleteUsers_각_id에_대해_삭제_수행() {
        User u1 = user(1L, "유저1");
        User u2 = user(2L, "유저2");
        given(userRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(u1, u2));

        userAdminService.bulkDeleteUsers(List.of(1L, 2L));

        verify(cascadeDeleteService).delete(u1);
        verify(cascadeDeleteService).delete(u2);
    }

    @Test
    void bulkDeleteUsers_존재하지_않는_id는_건너뛰고_나머지만_삭제() {
        User u1 = user(1L, "유저1");
        given(userRepository.findAllById(List.of(1L, 999L))).willReturn(List.of(u1));

        userAdminService.bulkDeleteUsers(List.of(1L, 999L));

        verify(cascadeDeleteService).delete(u1);
    }

    // ── updateUserRole ────────────────────────────────────────────────

    @Test
    void 사용자_역할_변경_성공() {
        User user = user(1L, "유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userAdminService.updateUserRole(1L, UserRole.ADMIN);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    // ── banUser / unbanUser ──────────────────────────────────────────

    @Test
    void 사용자_정지_성공() {
        User user = user(1L, "유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        userAdminService.banUser(1L, 7, "부적절한 게시물");

        assertThat(user.isBanned()).isTrue();
    }

    @Test
    void 인증안된_상태에서_정지시_관리자명_없이_처리() {
        User user = user(1L, "유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userAdminService.banUser(1L, 7, "부적절한 게시물");

        assertThat(user.isBanned()).isTrue();
        assertThat(user.getBannedBy()).isNull();
    }

    @Test
    void 사용자_정지_해제_성공() {
        User user = user(1L, "유저");
        user.ban(7, "사유", "admin");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userAdminService.unbanUser(1L);

        assertThat(user.isBanned()).isFalse();
    }

    // ── getAllUsersForExport ─────────────────────────────────────────

    @Test
    void 전체_사용자_내보내기() {
        given(userRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(user(1L, "유저1"))));

        List<UserResponseDto> result = userAdminService.getAllUsersForExport();

        assertThat(result).hasSize(1);
    }

    @Test
    void 전체_사용자_내보내기_여러_배치_순회() {
        User u1 = user(1L, "유저1");
        User u2 = user(2L, "유저2");
        Page<User> firstBatch = new PageImpl<>(List.of(u1), Pageable.ofSize(1), 2);
        Page<User> secondBatch = new PageImpl<>(List.of(u2), Pageable.ofSize(1).withPage(1), 2);
        given(userRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .willReturn(firstBatch, secondBatch);

        List<UserResponseDto> result = userAdminService.getAllUsersForExport();

        assertThat(result).hasSize(2);
    }
}
