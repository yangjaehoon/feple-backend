package com.feple.feple_backend.admin;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.function.Consumer;

public final class AdminActionUtils {

    private AdminActionUtils() {}

    @FunctionalInterface
    public interface AdminAction {
        void run() throws Exception;
    }

    /**
     * 관리자 컨트롤러의 표준 try-catch 패턴 추상화:
     * - 성공 시 successMessage flash attribute 설정 (successMsg가 null이면 생략)
     * - IllegalArgumentException: e.getMessage()를 errorMessage로 노출 (서비스 검증 메시지)
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
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            onError.accept(e);
            ra.addFlashAttribute("errorMessage", failMsg);
        }
    }

    public static String toRedirect(UriComponentsBuilder builder, String keyword) {
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        return "redirect:" + builder.build().toUriString();
    }
}
