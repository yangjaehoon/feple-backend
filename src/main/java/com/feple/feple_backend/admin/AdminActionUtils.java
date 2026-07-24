package com.feple.feple_backend.admin;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * - IllegalArgumentException | NoSuchElementException: e.getMessage()를 errorMessage로 노출
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
        } catch (IllegalArgumentException | NoSuchElementException e) {
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
        } catch (IllegalArgumentException | NoSuchElementException e) {
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
     * 선택된 id가 없으면 errorMessage flash attribute를 설정하고 redirectUrl로 이동한다.
     * 선택된 id가 있으면 null을 반환하므로, 호출부는 `if (result != null) return result;` 형태로 사용한다.
     */
    public static String requireNonEmptySelection(List<Long> ids, String redirectUrl, RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) return null;
        ra.addFlashAttribute("errorMessage", AdminConstants.MSG_EMPTY_SELECTION);
        return redirectUrl;
    }

    /**
     * 관리자 컨트롤러의 표준 GET try-catch 패턴 (model 채우고 뷰 반환):
     * - 성공 시 viewName 반환
     * - IllegalArgumentException | NoSuchElementException: e.getMessage()를 errorMessage로 설정 후 fallbackRedirect 반환
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
        } catch (IllegalArgumentException | NoSuchElementException e) {
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
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        return "redirect:" + builder.build().toUriString();
    }

    /**
     * status + page + keyword 조합의 목록 페이지 redirect URL을 생성한다.
     * status 파라미터 값이 없는 경우(null/빈 문자열)도 그대로 전달된다.
     */
    public static String listRedirect(String basePath, Object status, int page, String keyword) {
        return toRedirect(
                UriComponentsBuilder.fromPath(basePath)
                        .queryParam("status", status)
                        .queryParam("page", page),
                keyword);
    }
}
