package com.feple.feple_backend.userblock.service;

import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.dto.BlockedUserDto;
import com.feple.feple_backend.userblock.entity.UserBlock;
import com.feple.feple_backend.userblock.repository.UserBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceImplTest {

    @Mock UserBlockRepository blockRepository;
    @Mock UserRepository userRepository;

    @InjectMocks UserBlockServiceImpl service;

    private User user(Long id, String nickname) {
        return User.builder().id(id).oauthId("o" + id).nickname(nickname).build();
    }

    // ── block ─────────────────────────────────────────────────────────────

    @Test
    void 차단_성공() {
        User blocker = user(1L, "차단자");
        User blocked = user(2L, "피차단자");
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(blocker));
        given(userRepository.findById(2L)).willReturn(Optional.of(blocked));

        service.block(1L, 2L);

        ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
        then(blockRepository).should().save(captor.capture());
        assertThat(captor.getValue().getBlockerId()).isEqualTo(1L);
        assertThat(captor.getValue().getBlockedId()).isEqualTo(2L);
    }

    @Test
    void 차단_자기_자신이면_예외() {
        assertThatThrownBy(() -> service.block(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 자신을 차단할 수 없습니다.");

        then(blockRepository).shouldHaveNoInteractions();
    }

    @Test
    void 차단_이미_차단한_사용자면_예외() {
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> service.block(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 차단한 사용자입니다.");

        then(blockRepository).should(org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 차단_대상_사용자_없으면_예외() {
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L, "차단자")));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.block(1L, 2L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── unblock ───────────────────────────────────────────────────────────

    @Test
    void 차단해제_성공() {
        given(blockRepository.deleteByBlockerIdAndBlockedId(1L, 2L)).willReturn(1);

        service.unblock(1L, 2L);

        then(blockRepository).should().deleteByBlockerIdAndBlockedId(1L, 2L);
    }

    @Test
    void 차단해제_내역_없으면_예외() {
        given(blockRepository.deleteByBlockerIdAndBlockedId(1L, 2L)).willReturn(0);

        assertThatThrownBy(() -> service.unblock(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차단 내역이 없습니다.");
    }

    // ── getBlockedUsers ───────────────────────────────────────────────────

    @Test
    void 차단_목록_조회() {
        User blocker = user(1L, "차단자");
        User blocked = user(2L, "피차단자");
        UserBlock block = UserBlock.of(blocker, blocked);
        given(blockRepository.findByBlockerIdOrderByCreatedAtDesc(1L)).willReturn(List.of(block));

        List<BlockedUserDto> result = service.getBlockedUsers(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        assertThat(result.get(0).getNickname()).isEqualTo("피차단자");
    }

    // ── isBlocked ─────────────────────────────────────────────────────────

    @Test
    void 차단_여부_확인() {
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        assertThat(service.isBlocked(1L, 2L)).isTrue();
    }

    // ── getBlockedIds ─────────────────────────────────────────────────────

    @Test
    void 차단한_사용자_ID_목록_조회() {
        given(blockRepository.findBlockedIdsByBlockerId(1L)).willReturn(List.of(2L, 3L));

        assertThat(service.getBlockedIds(1L)).containsExactly(2L, 3L);
    }

    // ── removeAllByUser ───────────────────────────────────────────────────

    @Test
    void 회원_탈퇴시_차단_내역_전체_삭제() {
        service.removeAllByUser(1L);

        then(blockRepository).should().deleteAllByUserId(1L);
    }
}
