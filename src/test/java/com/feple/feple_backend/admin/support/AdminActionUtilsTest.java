package com.feple.feple_backend.admin.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.util.UriComponentsBuilder;

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
    void tryAction_InvalidRequestException은_errorMessage에_메시지_노출() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(
                () -> { throw new InvalidRequestException("잘못된 입력입니다."); },
                "성공", e -> {}, "실패", ra);

        assertThat(flash(ra, "errorMessage")).isEqualTo("잘못된 입력입니다.");
        assertThat(ra.getFlashAttributes()).doesNotContainKey("successMessage");
    }

    @Test
    void tryAction_ResourceNotFoundException은_errorMessage에_메시지_노출() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(
                () -> { throw new ResourceNotFoundException("항목을 찾을 수 없습니다."); },
                "성공", e -> {}, "실패", ra);

        assertThat(flash(ra, "errorMessage")).isEqualTo("항목을 찾을 수 없습니다.");
    }

    // 순수 JDK 예외는 내부 구현("No value present", enum 상수명 등)이 샐 수 있어 메시지를 노출하지 않고
    // failMsg로 마스킹한다. 사용자용 메시지는 InvalidRequestException / ResourceNotFoundException.
    @Test
    void tryAction_순수_IllegalArgumentException은_failMsg로_마스킹() {
        RedirectAttributesModelMap ra = ra();
        AtomicReference<Exception> captured = new AtomicReference<>();

        AdminActionUtils.tryAction(
                () -> { throw new IllegalArgumentException("No enum constant Foo.BAR"); },
                "성공", captured::set, "처리 중 오류가 발생했습니다.", ra);

        assertThat(captured.get()).isInstanceOf(IllegalArgumentException.class);
        assertThat(flash(ra, "errorMessage")).isEqualTo("처리 중 오류가 발생했습니다.");
    }

    @Test
    void tryAction_순수_NoSuchElementException은_failMsg로_마스킹() {
        RedirectAttributesModelMap ra = ra();

        AdminActionUtils.tryAction(
                () -> { throw new NoSuchElementException("No value present"); },
                "성공", e -> {}, "처리 중 오류가 발생했습니다.", ra);

        assertThat(flash(ra, "errorMessage")).isEqualTo("처리 중 오류가 발생했습니다.");
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
    void tryRender_InvalidRequestException은_fallback_반환_및_errorMessage_설정() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.tryRender(
                () -> { throw new InvalidRequestException("존재하지 않는 페스티벌"); },
                "admin/festival/detail",
                e -> {}, "실패", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(flash(ra, "errorMessage")).isEqualTo("존재하지 않는 페스티벌");
    }

    @Test
    void tryRender_ResourceNotFoundException은_fallback_반환() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.tryRender(
                () -> { throw new ResourceNotFoundException("없음"); },
                "admin/festival/detail",
                e -> {}, "실패", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(flash(ra, "errorMessage")).isEqualTo("없음");
    }

    @Test
    void tryRender_순수_NoSuchElementException은_failMsg로_마스킹() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.tryRender(
                () -> { throw new NoSuchElementException("No value present"); },
                "admin/festival/detail",
                e -> {}, "정보를 불러오는 중 오류가 발생했습니다.", "redirect:/admin/festivals", ra);

        assertThat(result).isEqualTo("redirect:/admin/festivals");
        assertThat(flash(ra, "errorMessage")).isEqualTo("정보를 불러오는 중 오류가 발생했습니다.");
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

    // ── listUrl ───────────────────────────────────────────────────────────────

    @Test
    void listUrl_listRedirect와_동일한_쿼리스트링을_redirect_접두사_없이_만든다() {
        String url = AdminActionUtils.listUrl("/admin/certifications", "PENDING", 2, null);
        String redirect = AdminActionUtils.listRedirect("/admin/certifications", "PENDING", 2, null);

        assertThat(url).isEqualTo("/admin/certifications?status=PENDING&page=2");
        assertThat(redirect).isEqualTo("redirect:" + url);
    }

    @Test
    void listUrl_status가_빈_문자열이어도_파라미터를_유지한다() {
        assertThat(AdminActionUtils.listUrl("/admin/certifications", "", 0, "홍길동"))
                .startsWith("/admin/certifications?status=&page=0&keyword=");
        assertThat(AdminActionUtils.listUrl("/admin/certifications", "", 0, "홍길동"))
                .doesNotContain("홍길동");
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

    // ── requireValidSelection ────────────────────────────────────────────────

    @Test
    void requireValidSelection_정상_선택이면_null_반환() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.requireValidSelection(
                List.of(1L, 2L, 3L), "redirect:/admin/users", ra);

        assertThat(result).isNull();
        assertThat(ra.getFlashAttributes()).isEmpty();
    }

    @Test
    void requireValidSelection_ids_null이면_선택없음_메시지_후_redirect() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.requireValidSelection(null, "redirect:/admin/users", ra);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(flash(ra, "errorMessage")).isEqualTo(AdminConstants.MSG_EMPTY_SELECTION);
    }

    @Test
    void requireValidSelection_ids_비어있으면_선택없음_메시지_후_redirect() {
        RedirectAttributesModelMap ra = ra();

        String result = AdminActionUtils.requireValidSelection(
                List.of(), "redirect:/admin/users", ra);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(flash(ra, "errorMessage")).isEqualTo(AdminConstants.MSG_EMPTY_SELECTION);
    }

    @Test
    void requireValidSelection_상한_이하면_통과() {
        RedirectAttributesModelMap ra = ra();
        List<Long> ids = LongStream
                .rangeClosed(1, AdminConstants.BULK_ACTION_MAX_IDS).boxed().toList();

        String result = AdminActionUtils.requireValidSelection(ids, "redirect:/admin/users", ra);

        assertThat(result).isNull();
    }

    @Test
    void requireValidSelection_상한_초과하면_안내_메시지_후_redirect() {
        RedirectAttributesModelMap ra = ra();
        List<Long> ids = LongStream
                .rangeClosed(1, AdminConstants.BULK_ACTION_MAX_IDS + 1).boxed().toList();

        String result = AdminActionUtils.requireValidSelection(ids, "redirect:/admin/users", ra);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(flash(ra, "errorMessage")).isEqualTo(AdminConstants.MSG_BULK_TOO_MANY);
    }

    @Test
    void describeIds_건수와_id_목록_문자열() {
        assertThat(AdminActionUtils.describeIds(List.of(12L, 45L, 78L)))
                .isEqualTo("3건 [12, 45, 78]");
    }
}
