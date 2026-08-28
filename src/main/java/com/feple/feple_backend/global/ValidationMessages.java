package com.feple.feple_backend.global;

/**
 * 여러 DTO·컨트롤러 파라미터에서 문자열이 그대로 반복되던 Bean Validation 메시지 모음.
 * (필드 하나에서만 쓰는 메시지는 해당 DTO에 그대로 둔다.)
 */
public final class ValidationMessages {

    private ValidationMessages() {}

    public static final String TITLE_REQUIRED = "제목은 필수입니다.";
    public static final String CONTENT_REQUIRED = "내용은 필수입니다.";
    public static final String CONTENT_BLANK = "내용을 입력해주세요.";
    public static final String CONTENT_TYPE_REQUIRED = "Content-Type은 필수입니다.";
    public static final String FILE_EXTENSION_REQUIRED = "파일 확장자는 필수입니다.";
    public static final String FESTIVAL_ID_REQUIRED = "페스티벌 ID는 필수입니다.";
    public static final String VISIBILITY_REQUIRED = "공개 범위는 필수입니다.";
    public static final String SONG_TITLE_REQUIRED = "곡 제목을 입력해주세요.";
    public static final String TOKEN_REQUIRED = "토큰이 필요합니다.";

    public static final String TITLE_MAX_100 = "제목은 100자 이내로 입력해주세요.";
    public static final String DESCRIPTION_MAX_500 = "설명은 500자 이내로 입력해주세요.";
    public static final String NOTE_MAX_255 = "메모는 255자 이내로 입력해주세요.";
    public static final String COMMENT_MAX_1000 = "댓글은 1000자 이내로 입력해주세요.";
    public static final String POST_CONTENT_MAX_5000 = "내용은 5000자 이내로 입력해주세요.";
    public static final String DIARY_CONTENT_MAX_2000 = "내용은 2000자 이내로 작성해주세요.";
    public static final String IMAGE_URL_TOO_LONG = "이미지 URL이 너무 깁니다.";
}
