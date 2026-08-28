package com.feple.feple_backend.global;

/**
 * 신고 대상 종류 식별자. 각 신고 도메인 서비스의 {@code getReportType()}와
 * 관리자 신고 화면/CSV의 타입별 디스패치가 같은 값을 써야 하므로 공용으로 둔다.
 *
 * <p>열거형이 아닌 이유: 컨트롤러의 {@code Map<String, ...>} 디스패치와
 * {@code @RequestParam}/{@code @ModelAttribute} 문자열 바인딩에 그대로 쓰이며,
 * 그 전환은 별도 작업으로 미룬다.
 */
public final class ReportTypes {

    private ReportTypes() {}

    public static final String POST = "post";
    public static final String COMMENT = "comment";
    public static final String PHOTO = "photo";
    public static final String USER = "user";
}
