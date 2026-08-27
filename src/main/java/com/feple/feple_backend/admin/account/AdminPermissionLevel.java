package com.feple.feple_backend.admin.account;

/**
 * 관리자 페이지 접근 권한의 수준.
 *
 * <ul>
 *   <li>{@link #READ}  — 목록·상세 조회(GET)만 가능</li>
 *   <li>{@link #WRITE} — 조회 + 생성/수정/삭제 등 변경(POST 등) 가능. WRITE는 READ를 포함한다.</li>
 * </ul>
 */
public enum AdminPermissionLevel {
    READ,
    WRITE
}
