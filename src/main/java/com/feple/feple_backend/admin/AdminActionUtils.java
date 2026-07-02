package com.feple.feple_backend.admin;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public final class AdminActionUtils {

    private AdminActionUtils() {}

    @FunctionalInterface
    public interface AdminAction {
        void run() throws Exception;
    }

    /**
     * 관리자 컨트롤러의 표준 POST try-catch 패턴:
     * - 성공 시 successMessage flash attribute 설정 (successMsg가 null이면 생략)
     * - IllegalArgumentException | NoSuchElementException: e.getMessage()를 errorMessage로 노출
     * - 그 외 Exception: onError 콜백(log.error)을 호출하고 failMsg를 errorMessage로 노출
     */
    public static void tryAction(AdminAction action,
                                 String successMsg,
                                 Consumer<Exception> onError,
                                 String failMsg,
                                 RedirectAttributes ra) {
        try {
            action.run();
            if (successMsg != null) ra.addFlashAttribute("successMessage", successMsg);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            onError.accept(e);
            ra.addFlashAttribute("errorMessage", failMsg);
        }
    }

    /**
     * 관리자 컨트롤러의 표준 GET try-catch 패턴 (model 채우고 뷰 반환):
     * - 성공 시 viewName 반환
     * - IllegalArgumentException | NoSuchElementException: e.getMessage()를 errorMessage로 설정 후 fallbackRedirect 반환
     * - 그 외 Exception: onError 콜백 호출 후 failMsg를 errorMessage로 설정, fallbackRedirect 반환
     */
    public static String tryRender(AdminAction action,
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
