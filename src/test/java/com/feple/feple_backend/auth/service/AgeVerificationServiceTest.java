package com.feple.feple_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.exception.AgeRestrictedException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.entity.WithdrawalReason;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgeVerificationServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @InjectMocks AgeVerificationService ageVerificationService;

    private User activeUser() {
        return User.builder().id(1L).oauthId("o1").nickname("u1").role(UserRole.USER).build();
    }

    @Test
    void 만_14세_이상이면_생년월일_저장() {
        given(userRepository.findById(1L)).willReturn(Optional.of(activeUser()));
        LocalDate birthDate = LocalDate.now().minusYears(20);

        ageVerificationService.submitBirthDate(1L, birthDate);

        verify(userService).recordBirthDate(1L, birthDate);
        verify(userService, never()).deleteUser(anyLong(), any(), any());
    }

    @Test
    void 만_14세_미만이면_계정_삭제_후_AgeRestrictedException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(activeUser()));
        LocalDate birthDate = LocalDate.now().minusYears(10);

        assertThatThrownBy(() -> ageVerificationService.submitBirthDate(1L, birthDate))
                .isInstanceOf(AgeRestrictedException.class);

        ArgumentCaptor<WithdrawalReason> reason = ArgumentCaptor.forClass(WithdrawalReason.class);
        verify(userService).deleteUser(eq(1L), reason.capture(), isNull());
        assertThat(reason.getValue()).isEqualTo(WithdrawalReason.AGE_RESTRICTED);
        verify(userService, never()).recordBirthDate(anyLong(), any());
    }

    @Test
    void 미래_생년월일이면_InvalidRequestException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(activeUser()));

        assertThatThrownBy(() -> ageVerificationService.submitBirthDate(1L, LocalDate.now().plusDays(1)))
                .isInstanceOf(InvalidRequestException.class);
        verify(userService, never()).recordBirthDate(anyLong(), any());
    }

    @Test
    void 이미_생년월일이_있으면_재제출은_무시() {
        User verified = User.builder().id(1L).oauthId("o1").nickname("u1").role(UserRole.USER)
                .birthDate(LocalDate.of(2000, 1, 1)).build();
        given(userRepository.findById(1L)).willReturn(Optional.of(verified));

        // 실수로 만 14세 미만 날짜를 다시 넣어도 계정이 파기되면 안 된다.
        ageVerificationService.submitBirthDate(1L, LocalDate.now().minusYears(5));

        verify(userService, never()).deleteUser(anyLong(), any(), any());
        verify(userService, never()).recordBirthDate(anyLong(), any());
    }

    @Test
    void 이미_삭제된_계정이면_AgeRestrictedException() {
        User deleted = User.builder().id(1L).oauthId("o1").nickname("u1").role(UserRole.USER)
                .deletedAt(LocalDateTime.now()).build();
        given(userRepository.findById(1L)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> ageVerificationService.submitBirthDate(1L, LocalDate.now().minusYears(20)))
                .isInstanceOf(AgeRestrictedException.class);
    }
}
