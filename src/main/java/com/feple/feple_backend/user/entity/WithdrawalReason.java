package com.feple.feple_backend.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WithdrawalReason {
    RARELY_USED("자주 사용하지 않아요"),
    NOT_ENOUGH_CONTENT("원하는 아티스트/페스티벌 정보가 없어요"),
    BUGS_OR_ERRORS("오류가 자주 발생해요"),
    PRIVACY_CONCERN("개인정보가 걱정돼요"),
    OTHER("기타");

    private final String displayName;
}
