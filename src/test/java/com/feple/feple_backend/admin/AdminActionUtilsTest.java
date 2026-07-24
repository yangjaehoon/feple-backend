package com.feple.feple_backend.admin;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AdminActionUtilsTest {

    private static RedirectAttributesModelMap ra() {
        return new RedirectAttributesModelMap();
    }

    /** flash attribute 값 조회 헬퍼 — Map<String,?> 와일드카드 타입 문제 우회 */
    private static Object flash(RedirectAttributesModelMap ra, String key) {
        return ra.getFlashAttributes().get(key);
    }

    // ── tryAction ─────────────────────────────────────────────────────────────

    @Test
    void tryAction_성공시_successMessage_flash_설정() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(() -> {}, "저장되었습니다.", e -> {}, "실패", ra);

        assertThat(flash(ra, "successMessage")).isEqualTo("저장되었습니다.");
    }

    @Test
    void tryAction_successMsg_null이면_successMessage_설정_안함() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(() -> {}, null, e -> {}, "실패", ra);

        assertThat(ra.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    @Test
    void tryAction_IllegalArgumentException은_errorMessage에_메시지_노출() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(
                () -> { throw new IllegalArgumentException("잘못된 입력입니다."); },
                "성공", e -> {}, "실패", ra);

        assertThat(flash(ra, "errorMessage")).isEqualTo("잘못된 입력입니다.");
        assertThat(ra.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    @Test
    void tryAction_NoSuchElementException은_errorMessage에_메시지_노출() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(
                () -> { throw new NoSuchElementException("항목을 찾을 수 없습니다."); },
                "성공", e -> {}, "실패", ra);

        assertThat(flash(ra, "errorMessage")).isEqualTo("항목을 찾을 수 없습니다.");
    }

    @Test
    void tryAction_OptimisticLockingFailureException은_고정_안내_메시지_노출_raw_메시지_미노출() {
        RedirectAttributesModelMap ra = ra();
        AtomicReference<Exception> captured = new AtomicReference<>();

        AdminActionUtils.tryAction(
                () -> { throw new OptimisticLockingFailureException("Row was updated by another transaction"); },
                "성공", captured::set, "실패", ra);

        assertThat(captured.get()).isNull();
        assertThat(flash(ra, "errorMessage")).isEqualTo("다른 관리자가 방금 먼저 수정했습니다. 새로고침 후 다시 시도해주세요.");
    }

    @Test
    void tryAction_기타_Exception은_onError_호출하고_failMsg_설정() {
        RedirectAttributesModelMap ra = ra();
        AtomicReference<Exception> captured = new AtomicReference<>();
        RuntimeException cause = new RuntimeException("DB 오류");

        AdminActionUtils.tryAction(
                () -> { throw cause; },
                "성공", captured::set, "처리 중 오류가 발생했습니다.", ra);

        assertThat(captured.get()).isSameAs(cause);
        assertThat(flash(ra, "errorMessage")).isEqualTo("처리 중 오류가 발생했습니다.");
        assertThat(ra.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    // ── tryRender ─────────────────────────────────────────────────────────────

    @Test
    void tryRender_성공시_viewName_반환() {
        String result = AdminActionUtils.tryRender(
                () -> {}, "admin/festival/detail",
                e -> {}, "실패", "redirect:/admin/festivals", ra());

        assertThat(result).isEqualTo("admin/festival/detail");
    }

    @Test
    void tryRender_성공시_errorMessage_없음() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryRender(
                () -> {}, "admin/festival/detail",
                e -> {}, "실패", "redirect:/admin/festivals", ra);

        assertThat(ra.getFlashAttributes()).doesNotContainKey("errorMessage");
    }

    @Test
    void tryRender_IllegalArgumentException은_fallback_반환_및_errorMessage_설정() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.tryRender(
                () -> { throw new IllegalArgumentException("존재하지 않는 페스티벌"); },
                "admin/festival/detail",
                e -> {}, "실패", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(flash(ra, "errorMessage")).isEqualTo("존재하지 않는 페스티벌");
    }

    @Test
    void tryRender_NoSuchElementException은_fallback_반환() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.tryRender(
                () -> { throw new NoSuchElementException("없음"); },
                "admin/festival/detail",
                e -> {}, "실패", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(flash(ra, "errorMessage")).isEqualTo("없음");
    }

    @Test
    void tryRender_OptimisticLockingFailureException은_고정_안내_메시지_후_fallback_반환() {
        RedirectAttributesModelMap ra = ra();
        AtomicReference<Exception> captured = new AtomicReference<>();

        String result = AdminActionUtils.tryRender(
                () -> { throw new OptimisticLockingFailureException("Row was updated by another transaction"); },
                "admin/festival/detail",
                captured::set, "실패", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(captured.get()).isNull();
        assertThat(flash(ra, "errorMessage")).isEqualTo("다른 관리자가 방금 먼저 수정했습니다. 새로고침 후 다시 시도해주세요.");
    }

    @Test
    void tryRender_기타_Exception은_onError_호출_후_fallback_반환() {
        RedirectAttributesModelMap ra = ra();
        AtomicReference<Exception> captured = new AtomicReference<>();

        String result = AdminActionUtils.tryRender(
                () -> { throw new RuntimeException("서버 오류"); },
                "admin/festival/detail",
                captured::set, "처리 중 오류가 발생했습니다.", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(captured.get()).isInstanceOf(RuntimeException.class);
        assertThat(flash(ra, "errorMessage")).isEqualTo("처리 중 오류가 발생했습니다.");
    }

    // ── toRedirect ────────────────────────────────────────────────────────────

    @Test
    void toRedirect_keyword_null이면_쿼리파라미터_없이_redirect() {
        String result = AdminActionUtils.toRedirect(
                UriComponentsBuilder.fromPath("/admin/festivals"), null);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
    }

    @Test
    void toRedirect_keyword_공백이면_쿼리파라미터_없이_redirect() {
        String result = AdminActionUtils.toRedirect(
                UriComponentsBuilder.fromPath("/admin/festivals"), "  ");

        assertThat(result).isEqualTo("redirect:/admin/festivals");
    }

    @Test
    void toRedirect_keyword_있으면_쿼리파라미터_포함() {
        String result = AdminActionUtils.toRedirect(
                UriComponentsBuilder.fromPath("/admin/festivals"), "feple");

        assertThat(result).isEqualTo("redirect:/admin/festivals?keyword=feple");
    }

    // ── listRedirect ──────────────────────────────────────────────────────────

    @Test
    void listRedirect_keyword_없으면_status_page_포함() {
        String result = AdminActionUtils.listRedirect("/admin/reports", "PENDING", 2, null);

        assertThat(result).isEqualTo("redirect:/admin/reports?status=PENDING&page=2");
    }

    @Test
    void listRedirect_keyword_있으면_status_page_keyword_모두_포함() {
        String result = AdminActionUtils.listRedirect("/admin/reports", "PENDING", 0, "feple");

        assertThat(result).isEqualTo("redirect:/admin/reports?status=PENDING&page=0&keyword=feple");
    }

    @Test
    void listRedirect_status_null이어도_status_파라미터_포함() {
        // null status → UriComponentsBuilder 가 값 없이 키만 추가: ?status&page=0
        String result = AdminActionUtils.listRedirect("/admin/reports", null, 0, null);

        assertThat(result).isEqualTo("redirect:/admin/reports?status&page=0");
    }
}
