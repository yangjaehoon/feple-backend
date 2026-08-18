package com.feple.feple_backend.user.dto;

import com.feple.feple_backend.user.entity.WithdrawalReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotNull(message = "탈퇴 사유를 선택해주세요.") WithdrawalReason reason,
        @Size(max = 300, message = "상세 사유는 300자 이하로 입력해주세요.") String detail) {}
