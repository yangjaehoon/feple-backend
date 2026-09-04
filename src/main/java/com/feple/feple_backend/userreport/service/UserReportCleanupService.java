package com.feple.feple_backend.userreport.service;

import com.feple.feple_backend.userreport.repository.UserReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 완전 삭제(hardDelete) 시 user_report 도메인의 잔여 참조를 비우는 전용 진입점.
 *
 * <p>{@link UserReportService}에 두지 않는 이유: 그 클래스가 {@code UserAdminService}에 의존하는데,
 * {@code UserAdminServiceImpl} → {@code UserCascadeDeleteService} → (이 정리 호출)로 이어져
 * 순환 의존이 생긴다. 정리 책임만 별도 빈으로 떼어내면 {@code UserReportRepository}에만
 * 의존하므로 순환이 끊긴다.
 */
@Service
@RequiredArgsConstructor
public class UserReportCleanupService {

    private final UserReportRepository userReportRepository;

    /** 이 유저가 신고자이거나 피신고자인 신고 행을 모두 제거한다 (reporter_id·target_id 둘 다 users FK RESTRICT). */
    @Transactional
    public void removeAllInvolvingUser(Long userId) {
        userReportRepository.deleteByUserInvolved(userId);
    }
}
