package com.feple.feple_backend.user.dto;

import com.feple.feple_backend.user.entity.WithdrawalReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotNull(message = "탈퇴 사유를 선택해주세요.") WithdrawalReason reason,
        @Size(max = 300, message = "상세 사유는 300자 이하로 입력해주세요.") String detail) {

    // AGE_RESTRICTED는 나이 확인 시스템이 계정을 파기할 때만 쓰는 사유 — 자진 탈퇴 입력으로는 허용하지 않는다.
    @AssertTrue(message = "탈퇴 사유를 선택해주세요.")
    public boolean isSelectableReason() {
        return reason != WithdrawalReason.AGE_RESTRICTED;
    }
}
