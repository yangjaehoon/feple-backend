package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.ArtistNameFilter;
import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Mock UserCascadeDeleteService cascadeDeleteService;
    @Mock BadWordFilter badWordFilter;
    @Mock ArtistNameFilter artistNameFilter;

    @InjectMocks UserServiceImpl userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(Long id, String nickname) {
        return User.builder().id(id).oauthId("o" + id).nickname(nickname)
                .role(UserRole.USER).build();
    }

    private User userWithImage(Long id, String profileImageUrl) {
        return User.builder().id(id).oauthId("o" + id).nickname("user" + id)
                .profileImageUrl(profileImageUrl).role(UserRole.USER).build();
    }

    // ── checkNicknameAvailable ────────────────────────────────────────

    @Test
    void 닉네임_null이면_사용불가_반환() {
        Map<String, Object> result = userService.checkNicknameAvailable(null, null);

        assertThat(result.get("available")).isEqualTo(false);
    }

    @Test
    void 닉네임_공백이면_사용불가_반환() {
        Map<String, Object> result = userService.checkNicknameAvailable("   ", null);

        assertThat(result.get("available")).isEqualTo(false);
    }

    @Test
    void 닉네임_1자면_사용불가_반환() {
        Map<String, Object> result = userService.checkNicknameAvailable("a", null);

        assertThat(result.get("available")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("2자");
    }

    @Test
    void 닉네임_9자면_사용불가_반환() {
        Map<String, Object> result = userService.checkNicknameAvailable("abcdefghi", null);

        assertThat(result.get("available")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("8자");
    }

    @Test
    void 닉네임에_특수문자_포함시_사용불가_반환() {
        Map<String, Object> result = userService.checkNicknameAvailable("ab cd", null);

        assertThat(result.get("available")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("한글, 영문, 숫자, 밑줄");
    }

    @Test
    void 이미_사용중인_닉네임이면_사용불가_반환() {
        given(userRepository.existsByNickname("taker")).willReturn(true);

        Map<String, Object> result = userService.checkNicknameAvailable("taker", null);

        assertThat(result.get("available")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("이미 사용 중인");
    }

    @Test
    void 사용가능한_닉네임이면_사용가능_반환() {
        given(userRepository.existsByNickname("newbie")).willReturn(false);

        Map<String, Object> result = userService.checkNicknameAvailable("newbie", null);

        assertThat(result.get("available")).isEqualTo(true);
    }

    @Test
    void excludeUserId가_있으면_existsByNicknameAndIdNot_호출() {
        given(userRepository.existsByNicknameAndIdNot("newbie", 1L)).willReturn(false);

        Map<String, Object> result = userService.checkNicknameAvailable("newbie", 1L);

        assertThat(result.get("available")).isEqualTo(true);
        verify(userRepository).existsByNicknameAndIdNot("newbie", 1L);
    }

    // ── getUser ───────────────────────────────────────────────────────

    @Test
    void 존재하는_사용자_조회_성공() {
        User user = user(1L, "테스트유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponseDto dto = userService.getUser(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNickname()).isEqualTo("테스트유저");
    }

    @Test
    void 존재하지않는_사용자_조회시_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── resolveProfileImageUrl (getUser를 통해 검증) ──────────────────

    @Test
    void 프로필이미지_null이면_dto에_null_반환() {
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithImage(1L, null)));

        UserResponseDto dto = userService.getUser(1L);

        assertThat(dto.getProfileImageUrl()).isNull();
    }

    @Test
    void 프로필이미지_기본로고_url이면_null_반환() {
        given(userRepository.findById(1L))
                .willReturn(Optional.of(userWithImage(1L, "https://cdn.example.com/img/feple_logo.png")));

        UserResponseDto dto = userService.getUser(1L);

        assertThat(dto.getProfileImageUrl()).isNull();
    }

    @Test
    void 프로필이미지_http_url이면_그대로_반환() {
        given(userRepository.findById(1L))
                .willReturn(Optional.of(userWithImage(1L, "https://kakao.com/avatar.jpg")));

        UserResponseDto dto = userService.getUser(1L);

        assertThat(dto.getProfileImageUrl()).isEqualTo("https://kakao.com/avatar.jpg");
    }

    @Test
    void 프로필이미지_S3키면_빌드된_url_반환() {
        given(userRepository.findById(1L))
                .willReturn(Optional.of(userWithImage(1L, "uploads/user-1.jpg")));
        given(fileStorageService.buildUrl("uploads/user-1.jpg"))
                .willReturn("https://s3.example.com/uploads/user-1.jpg");

        UserResponseDto dto = userService.getUser(1L);

        assertThat(dto.getProfileImageUrl()).isEqualTo("https://s3.example.com/uploads/user-1.jpg");
    }

    // ── updateNickname ────────────────────────────────────────────────

    @Test
    void 닉네임_변경_성공() {
        User user = user(1L, "기존닉네임");
        given(userRepository.existsByNicknameAndIdNot("새닉네임", 1L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponseDto dto = userService.updateNickname(1L, "새닉네임");

        assertThat(dto.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void 중복_닉네임으로_변경시_예외() {
        given(userRepository.existsByNicknameAndIdNot("중복닉네임", 1L)).willReturn(true);

        assertThatThrownBy(() -> userService.updateNickname(1L, "중복닉네임"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 사용 중인");
    }

    @Test
    void 유효하지않은_닉네임으로_변경시_예외() {
        assertThatThrownBy(() -> userService.updateNickname(1L, "a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updateUserRole ────────────────────────────────────────────────

    @Test
    void 사용자_역할_변경_성공() {
        User user = user(1L, "유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.updateUserRole(1L, UserRole.ADMIN);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    // ── deleteUser / bulkDeleteUsers ──────────────────────────────────

    @Test
    void 사용자_삭제시_cascadeDeleteService에_위임() {
        User user = user(1L, "삭제유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(cascadeDeleteService).delete(user);
    }

    @Test
    void bulkDeleteUsers_각_id에_대해_삭제_수행() {
        User u1 = user(1L, "유저1");
        User u2 = user(2L, "유저2");
        given(userRepository.findById(1L)).willReturn(Optional.of(u1));
        given(userRepository.findById(2L)).willReturn(Optional.of(u2));

        userService.bulkDeleteUsers(List.of(1L, 2L));

        verify(cascadeDeleteService).delete(u1);
        verify(cascadeDeleteService).delete(u2);
    }

    // ── currentUserId ─────────────────────────────────────────────────

    @Test
    void 인증된_사용자의_id_반환() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("42", null, List.of()));

        long id = userService.currentUserId();

        assertThat(id).isEqualTo(42L);
    }

    @Test
    void 인증되지않은_경우_예외() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.currentUserId())
                .isInstanceOf(AuthenticationRequiredException.class);
    }
}
