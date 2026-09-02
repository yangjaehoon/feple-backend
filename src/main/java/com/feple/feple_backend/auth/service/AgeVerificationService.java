package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.global.exception.AgeRestrictedException;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.entity.AgePolicy;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.WithdrawalReason;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.service.UserService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 나이 확인 게이트. 첫 로그인 직후(또는 기존 유저의 최초 확인 시) 생년월일을 받아,
 * 만 {@link AgePolicy#MINIMUM_AGE}세 이상이면 저장하고, 미만이면 계정을 즉시 소프트
 * 삭제해 수집된 개인정보를 파기한다(Apple 심사 가이드라인 5.1.1 대응).
 *
 * <p>트랜잭션 경계 주의: 미달 판정 시 {@link UserService#deleteUser}로 삭제를 커밋한
 * 뒤에 예외를 던진다. 이 메서드 자체에 {@code @Transactional}을 걸면 마지막 예외가
 * 삭제까지 롤백시켜 계정이 남는다 — 그래서 여기서는 트랜잭션을 열지 않고 각 쓰기를
 * {@code UserService}의 개별 트랜잭션에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class AgeVerificationService {

    private static final LocalDate EARLIEST_BIRTH_DATE = LocalDate.of(1900, 1, 1);

    private final UserRepository userRepository;
    private final UserService userService;

    public void submitBirthDate(Long userId, LocalDate birthDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationRequiredException("로그인이 필요합니다."));
        if (user.isDeleted()) {
            throw new AgeRestrictedException("이용할 수 없는 계정입니다.");
        }
        // 이미 나이 확인을 마친 계정(또는 대상이 아닌 관리자·아티스트)의 재제출은 무시한다 —
        // 실수로 만 14세 미만 날짜를 다시 넣어 정상 계정이 파기되는 것을 막는다.
        if (!user.needsAgeVerification()) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today) || birthDate.isBefore(EARLIEST_BIRTH_DATE)) {
            throw new InvalidRequestException("생년월일이 올바르지 않습니다.");
        }

        if (AgePolicy.meetsMinimumAge(birthDate, today)) {
            userService.recordBirthDate(userId, birthDate);
            return;
        }

        // 소프트 삭제(deleteUser)가 리프레시 토큰 폐기까지 포함한다.
        userService.deleteUser(userId, WithdrawalReason.AGE_RESTRICTED, null);
        throw new AgeRestrictedException(
                "만 " + AgePolicy.MINIMUM_AGE + "세 미만은 커뮤니티 서비스를 이용할 수 없습니다.");
    }
}
