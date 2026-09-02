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
    OTHER("기타"),
    // 자진 탈퇴 사유가 아니라, 나이 확인에서 만 14세 미만으로 판정돼 시스템이 계정을
    // 소프트 삭제한 경우. 동일 소셜 계정 재로그인 시 이 사유로 재가입을 차단한다.
    AGE_RESTRICTED("만 14세 미만 이용 제한");

    private final String displayName;
}
