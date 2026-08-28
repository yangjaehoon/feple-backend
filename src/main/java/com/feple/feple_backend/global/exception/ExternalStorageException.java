package com.feple.feple_backend.global.exception;

/**
 * S3 등 외부 스토리지 연동 실패(업로드·삭제·조회 중 네트워크·권한·서비스 오류).
 *
 * <p>우리 서버 코드의 결함이 아니라 외부 의존성 장애이므로, 전역 핸들러는 이 예외를
 * {@code IllegalStateException}(500)이 아닌 502(Bad Gateway)로 매핑해 모니터링에서
 * 내부 서버 오류와 섞이지 않게 한다({@code WebClientException}과 동일한 취급).
 */
public class ExternalStorageException extends RuntimeException {

    public ExternalStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
