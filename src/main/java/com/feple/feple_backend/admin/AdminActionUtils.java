package com.feple.feple_backend.admin;

import com.feple.feple_backend.global.exception.BadWordException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

public final class AdminActionUtils {

    private AdminActionUtils() {}

    private static final String OPTIMISTIC_LOCK_MESSAGE =
            "다른 관리자가 방금 먼저 수정했습니다. 새로고침 후 다시 시도해주세요.";

    @FunctionalInterface
    public interface AdminTask {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface AdminTaskWithResult<T> {
        T run() throws Exception;
    }

    /**
     * 관리자 컨트롤러의 표준 POST try-catch 패턴:
     * - 성공 시 successMessage flash attribute 설정 (successMsg가 null이면 생략)
     * - InvalidRequestException | BadWordException | ResourceNotFoundException: e.getMessage()를 errorMessage로 노출 (안전한 도메인 메시지)
     * - OptimisticLockingFailureException(@Version 충돌): 고정 안내 메시지 노출 (raw 메시지 미노출)
     * - 그 외 Exception: onError 콜백(log.error)을 호출하고 failMsg를 errorMessage로 노출
     */
    public static void tryAction(AdminTask action,
                                 String successMsg,
                                 Consumer<Exception> onError,
                                 String failMsg,
                                 RedirectAttributes ra) {
        try {
            action.run();
            if (successMsg != null) ra.addFlashAttribute("successMessage", successMsg);
        } catch (InvalidRequestException | BadWordException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            ra.addFlashAttribute("errorMessage", OPTIMISTIC_LOCK_MESSAGE);
        } catch (Exception e) {
            onError.accept(e);
            ra.addFlashAttribute("errorMessage", failMsg);
        }
    }

    /**
     * tryAction과 동일한 표준 POST try-catch 패턴이되, action이 반환한 결과값으로
     * successMsg를 동적으로 계산해야 하는 경우(예: 처리 건수·저장 여부에 따라 메시지가 달라짐) 사용한다.
     * action이 예외 없이 완료됐을 때만 successMsgFn이 호출되므로, 실패 시 successMessage는 설정되지 않는다.
     * successMsgFn의 반환값이 null이면 successMessage flash attribute를 생략한다.
     */
    public static <T> void tryActionWithResult(AdminTaskWithResult<T> action,
                                               Function<T, String> successMsgFn,
                                               Consumer<Exception> onError,
                                               String failMsg,
                                               RedirectAttributes ra) {
        try {
            T result = action.run();
            String successMsg = successMsgFn.apply(result);
            if (successMsg != null) ra.addFlashAttribute("successMessage", successMsg);
        } catch (InvalidRequestException | BadWordException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            ra.addFlashAttribute("errorMessage", OPTIMISTIC_LOCK_MESSAGE);
        } catch (Exception e) {
            onError.accept(e);
            ra.addFlashAttribute("errorMessage", failMsg);
        }
    }

    /**
     * 일괄 작업(bulk action) 컨트롤러 메서드 시작부의 표준 가드 클로즈:
     * - 선택된 id가 없으면 "선택된 항목이 없습니다." errorMessage 설정 후 redirectUrl 반환
     * - 선택된 id가 {@link AdminConstants#BULK_ACTION_MAX_IDS}개를 초과하면 안내 메시지 설정 후 redirectUrl 반환
     * 정상 선택이면 null을 반환하므로, 호출부는 `if (result != null) return result;` 형태로 사용한다.
     */
    public static String requireValidSelection(List<Long> ids, String redirectUrl, RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", AdminConstants.MSG_EMPTY_SELECTION);
            return redirectUrl;
        }
        if (ids.size() > AdminConstants.BULK_ACTION_MAX_IDS) {
            ra.addFlashAttribute("errorMessage", AdminConstants.MSG_BULK_TOO_MANY);
            return redirectUrl;
        }
        return null;
    }

    /**
     * 일괄 작업 감사 로그의 detail 문자열에 넣을 "선택 건수 + id 목록" 표현.
     * 예) describeIds(List.of(12L, 45L, 78L)) → "3건 [12, 45, 78]"
     * 선택 상한(BULK_ACTION_MAX_IDS)이 걸려 있어 id 목록을 그대로 남겨도 로그 컬럼 길이에 여유가 있다.
     */
    public static String describeIds(List<Long> ids) {
        return ids.size() + "건 " + ids;
    }

    /**
     * 관리자 컨트롤러의 표준 GET try-catch 패턴 (model 채우고 뷰 반환):
     * - 성공 시 viewName 반환
     * - InvalidRequestException | BadWordException | ResourceNotFoundException: e.getMessage()를 errorMessage로 설정 후 fallbackRedirect 반환
     * - OptimisticLockingFailureException(@Version 충돌): 고정 안내 메시지 설정 후 fallbackRedirect 반환
     * - 그 외 Exception: onError 콜백 호출 후 failMsg를 errorMessage로 설정, fallbackRedirect 반환
     */
    public static String tryRender(AdminTask action,
                                   String viewName,
                                   Consumer<Exception> onError,
                                   String failMsg,
                                   String fallbackRedirect,
                                   RedirectAttributes ra) {
        try {
            action.run();
            return viewName;
        } catch (InvalidRequestException | BadWordException | ResourceNotFoundException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return fallbackRedirect;
        } catch (OptimisticLockingFailureException e) {
            ra.addFlashAttribute("errorMessage", OPTIMISTIC_LOCK_MESSAGE);
            return fallbackRedirect;
        } catch (Exception e) {
            onError.accept(e);
            ra.addFlashAttribute("errorMessage", failMsg);
            return fallbackRedirect;
        }
    }

    public static String toRedirect(UriComponentsBuilder builder, String keyword) {
        AdminUrlUtils.appendIfHasText(builder, "keyword", keyword);
        return "redirect:" + AdminUrlUtils.encode(builder);
    }

    /**
     * status + page (+ 선택적 keyword) 조합의 목록 페이지 URL. redirect: 접두사가 없는 형태로,
     * 상세 화면의 "돌아가기" 링크(returnUrl)에 쓴다.
     * status 값이 없어도(null/빈 문자열) 파라미터를 유지한다 — {@link #listRedirect}와 동일 규칙이라
     * 같은 화면의 redirect와 returnUrl이 항상 같은 쿼리스트링을 만든다.
     * 임의 키의 목록 URL이 필요하면 {@link AdminUrlUtils#listUrl(String, Object...)}를 쓴다(빈 값은 생략).
     */
    public static String listUrl(String basePath, Object status, int page, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath)
                .queryParam("status", status)
                .queryParam("page", page);
        AdminUrlUtils.appendIfHasText(builder, "keyword", keyword);
        return AdminUrlUtils.encode(builder);
    }

    /**
     * status + page + keyword 조합의 목록 페이지 redirect URL을 생성한다.
     * status 파라미터 값이 없는 경우(null/빈 문자열)도 그대로 전달된다.
     */
    public static String listRedirect(String basePath, Object status, int page, String keyword) {
        return "redirect:" + listUrl(basePath, status, page, keyword);
    }
}
