package com.feple.feple_backend.admin.support;

/**
 * page 파라미터 하나만 받는 관리자 목록/리다이렉트 화면용 파라미터 객체.
 * null·음수 page는 0으로 정규화한다(PageRequest.of가 음수에 예외를 던지는 것을 방어).
 */
public record PageParams(Integer page) {
    public PageParams {
        page = Math.max(0, AdminParamDefaults.orZero(page));
    }
}
