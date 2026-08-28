package com.feple.feple_backend.admin;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/**
 * 관리자 Thymeleaf 템플릿이 파싱·표현식 오류 없이 렌더되는지 검사하는 스모크 테스트.
 *
 * <p>실제 dialect(표준 + Spring Security)를 붙인 {@link SpringTemplateEngine}으로
 * {@code templates/admin/**}의 모든 페이지 템플릿을 렌더한다. 모델 데이터는 값의 "모양"이
 * 맞을 필요가 없도록 관대한 스텁({@link Self})으로 채운다 — 이 테스트가 잡으려는 건
 * 데이터 버그가 아니라 <b>템플릿 문법/표현식 오류</b>(잘못된 {@code th:attr}, 깨진 fragment
 * 참조, SpEL 오타 등)다. 컨트롤러 MockMvc 테스트는 뷰 이름만 검증하고 렌더는 안 하므로 사각지대.
 *
 * <p>{@link Self}에 대한 프로퍼티/메서드 접근은 커스텀 SpEL {@link PropertyAccessor}·
 * {@link MethodResolver}가 <b>멤버 이름</b>을 보고 타입에 맞는 값을 돌려준다 —
 * {@code createdAt} 류는 {@link LocalDateTime}, {@code xxxCount}는 숫자, {@code isXxx}는
 * {@code false}. {@link Self} 자체는 {@link Comparable}(항상 0)이고 {@code toString()}이
 * 빈 문자열이라 숫자 비교·문자열 연결이 표현식 평가 단계에서 터지지 않는다.
 */
class AdminTemplateRenderSmokeTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path ADMIN_DIR = TEMPLATES.resolve("admin");

    private static final LocalDateTime FIXED_DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

    // 페이지네이션 프래그먼트가 .content/.number/.totalPages 등을 실제로 계산하므로 Page여야 하는 키
    private static final Set<String> PAGE_KEYS = Set.of(
            "users", "posts", "artistsPage", "festivalsPage", "notices", "logs",
            "reports", "requests", "suggestions", "certifications");

    // 조회용 map 키. get()은 Self, getOrDefault(k, 0)은 기본값(숫자)을 돌려준다.
    private static final Set<String> LOOKUP_MAP_KEYS = Set.of(
            "reportCounts", "postCounts", "commentCounts", "checklistMap",
            "timetableAutoCompleteMap", "authorReportCounts", "photoUrls",
            "timetableByArtist", "counts", "setlistCounts");

    // JS 인라인([[${...}]])으로 JSON 직렬화되거나 SpEL 셀렉션(.?[])에 쓰여 실제 컬렉션이어야 하는 키
    private static final Set<String> EMPTY_LIST_KEYS = Set.of(
            "allArtists", "allFestivals", "booths", "rangeStats", "artists");

    // 최상위 모델 속성이라 커스텀 accessor를 못 타는, 숫자 비교(> 0 등)에 쓰이는 카운트류 키
    private static final Set<String> NUMBER_KEYS = Set.of(
            "sidebarReportCount", "sidebarCertCount", "sidebarSongRequestCount",
            "sidebarArtistSuggestionCount", "sidebarFestivalSuggestionCount",
            "sidebarSetlistRequestCount", "activeFestivalCount", "photoPendingCount",
            "postPendingCount", "commentPendingCount", "userPendingCount", "pendingCount",
            "totalCount", "processedSuggestionsTotal", "deviceCount", "statusCode", "nextCertId");

    // 최상위 모델 속성이라 커스텀 accessor를 못 타는, 문자열 연산 대상 키
    private static final Set<String> STRING_KEYS = Set.of(
            "keyword", "sort", "filter", "status", "type", "from", "to", "message",
            "backUrl", "returnUrl", "resetHref", "baseUrl", "extraParams", "pageTitle",
            "active", "errorMessage", "successMessage", "loginError", "googleMapsKey",
            "kakaoMapsKey", "returnKeyword", "returnStatus", "announcementStageName",
            "adminUsername");

    // Self 멤버 이름이 아래에 정확히 일치하면 boolean(false)로 취급. 거의 항상 술어인 이름만.
    private static final Set<String> BOOLEAN_MEMBER_NAMES = Set.of(
            "ended", "ongoing", "upcoming", "expired", "enabled", "disabled",
            "banned", "blinded", "pinned", "deleted", "admin", "artist", "permitted",
            "approved", "rejected", "anonymous", "permanentban",
            "contains", "containskey", "containsvalue", "equals", "matches",
            "startswith", "endswith");

    // th:field 로 폼 바인딩을 하는 뷰 → 실제 폼 DTO + BindingResult + RequestContext 필요
    private static final Set<String> FORM_VIEWS = Set.of(
            "admin/notice/create", "admin/notice/edit",
            "admin/artist/create", "admin/artist/edit",
            "admin/festival/create", "admin/festival/edit");

    private static SpringTemplateEngine engine;
    private static JakartaServletWebApplication webApp;
    private static MockServletContext servletContext;
    private static StandardEvaluationContext evaluationContext;

    @BeforeAll
    static void setUp() {
        servletContext = new MockServletContext();
        GenericWebApplicationContext appContext = new GenericWebApplicationContext(servletContext);
        // sec:authorize="hasAuthority(...)" 표현식 평가에 필요
        appContext.registerBean("webSecurityExpressionHandler",
                org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler.class);
        appContext.refresh();
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appContext);

        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(appContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new SpringSecurityDialect());

        webApp = JakartaServletWebApplication.buildApplication(servletContext);

        // 평범한 StandardEvaluationContext 를 "thymeleaf::EvaluationContext" 변수로 넘기면
        // thymeleaf 가 이를 ThymeleafEvaluationContextWrapper 로 감싸면서 일반 모드 접근자와
        // restricted 모드(th:href/th:src 안의 SpEL) 접근자 목록을 모두 이 인스턴스의 접근자로
        // 다시 만든다 → 커스텀 accessor/resolver 가 두 모드 모두에서 적용된다.
        evaluationContext = new StandardEvaluationContext();
        evaluationContext.addPropertyAccessor(new SelfPropertyAccessor());
        evaluationContext.addMethodResolver(new SelfMethodResolver());
        // Self 를 boolean/문자열/숫자 문맥에 넣었을 때(예: ${x.foo and y}, !${x.foo}) 강제변환 실패 방지
        org.springframework.core.convert.support.DefaultConversionService conversion =
                new org.springframework.core.convert.support.DefaultConversionService();
        conversion.addConverter(Self.class, Boolean.class, self -> Boolean.FALSE);
        conversion.addConverter(Self.class, String.class, self -> "");
        conversion.addConverter(Self.class, Integer.class, self -> 0);
        conversion.addConverter(Self.class, Long.class, self -> 0L);
        conversion.addConverter(Self.class, Double.class, self -> 0.0);
        evaluationContext.setTypeConverter(
                new org.springframework.expression.spel.support.StandardTypeConverter(conversion));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a",
                        AuthorityUtils.createAuthorityList(
                                "ROLE_ADMIN", "ROLE_SUPER_ADMIN",
                                "PERM_STATS_READ", "PERM_FESTIVALS_READ", "PERM_ARTISTS_READ",
                                "PERM_POSTS_READ", "PERM_NOTICES_READ", "PERM_CRAWL_READ",
                                "PERM_SONG_REQUESTS_READ", "PERM_CERTIFICATIONS_READ",
                                "PERM_REPORTS_READ", "PERM_USERS_READ", "PERM_BAD_WORDS_READ",
                                "PERM_LOGS_READ")));
    }

    @AfterAll
    static void tearDown() {
        SecurityContextHolder.clearContext();
    }

    static Stream<String> adminPageTemplates() throws IOException {
        try (Stream<Path> walk = Files.walk(ADMIN_DIR)) {
            return walk.filter(p -> p.toString().endsWith(".html"))
                    // fragment 정의 파일은 단독 렌더 대상이 아님 (페이지가 th:insert 하며 전이적으로 검사됨)
                    .filter(p -> !p.getFileName().toString().equals("fragments.html"))
                    .filter(p -> !p.getFileName().toString().contains("-fragment"))
                    .map(p -> "admin/" + ADMIN_DIR.relativize(p).toString()
                            .replace('\\', '/').replace(".html", ""))
                    .sorted()
                    .toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminPageTemplates")
    void 템플릿이_예외없이_렌더된다(String view) {
        MockHttpServletRequest req = new MockHttpServletRequest(servletContext);
        req.addPreferredLocale(Locale.KOREAN);
        MockHttpServletResponse res = new MockHttpServletResponse();
        WebContext ctx = new WebContext(
                webApp.buildExchange(req, res), Locale.KOREAN, model(view, req, res));

        assertThatCode(() -> engine.process(view, ctx))
                .as("템플릿 렌더 실패: %s", view)
                .doesNotThrowAnyException();
    }

    // ── 모델 ────────────────────────────────────────────────────────────

    private static Map<String, Object> model(
            String view, MockHttpServletRequest req, MockHttpServletResponse res) {
        Map<String, Object> m = new HashMap<>();
        for (String key : ALL_KEYS) {
            m.put(key, valueFor(key));
        }
        m.put("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "smoke"));
        m.put("errors", List.of());
        m.put("page", 0);
        // Self 멤버 접근을 이름 기반으로 해석하는 커스텀 SpEL accessor/resolver 주입
        m.put(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                evaluationContext);
        if (FORM_VIEWS.contains(view)) {
            addFormScaffolding(m, view, req, res);
        }
        return m;
    }

    private static Object valueFor(String key) {
        if (PAGE_KEYS.contains(key)) {
            return new PageImpl<>(List.of(Self.INSTANCE), PageRequest.of(0, 20), 1);
        }
        if (LOOKUP_MAP_KEYS.contains(key)) {
            return new LookupMap();
        }
        if (EMPTY_LIST_KEYS.contains(key)) {
            return List.of();
        }
        if (NUMBER_KEYS.contains(key)) {
            return 0;
        }
        if (STRING_KEYS.contains(key)) {
            return "";
        }
        return Self.INSTANCE;
    }

    /** th:field 뷰: 폼 DTO + BindingResult + Spring/Thymeleaf RequestContext 를 채운다. */
    private static void addFormScaffolding(Map<String, Object> m, String view,
            MockHttpServletRequest req, MockHttpServletResponse res) {
        String name = view.split("/")[1];
        Object formBean = switch (name) {
            case "notice" -> new NoticeRequestDto();
            case "festival" -> new FestivalRequestDto();
            case "artist" -> new ArtistRequestDto();
            default -> throw new IllegalStateException("no form bean for " + view);
        };
        m.put(name, formBean);
        m.put(BindingResult.MODEL_KEY_PREFIX + name,
                new BeanPropertyBindingResult(formBean, name));
        RequestContext rc = new RequestContext(req, res, servletContext, m);
        m.put("springRequestContext", rc);
        m.put("thymeleafRequestContext", new SpringWebMvcThymeleafRequestContext(rc, req));
    }

    /** admin 템플릿에서 ${xxx 로 참조되는 모든 최상위 식별자 */
    private static final List<String> ALL_KEYS = collectKeys();

    private static List<String> collectKeys() {
        try (Stream<Path> walk = Files.walk(ADMIN_DIR)) {
            Pattern p = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)");
            List<String> keys = new ArrayList<>();
            for (Path html : walk.filter(f -> f.toString().endsWith(".html")).toList()) {
                Matcher mt = p.matcher(Files.readString(html));
                while (mt.find()) keys.add(mt.group(1));
            }
            return keys.stream().distinct().sorted().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ── 관대한 스텁 ─────────────────────────────────────────────────────

    /**
     * 만능 값 스텁. {@code toString()}이 빈 문자열, {@link Comparable}로 항상 0 →
     * 문자열 연결·숫자 비교에서 예외가 안 난다. {@code th:each} 소스로 쓰이면 thymeleaf가
     * 단일 원소 리스트로 감싸 1회 순회한다. 프로퍼티/메서드 접근은 커스텀 SpEL 접근자가 처리.
     */
    static final class Self implements Comparable<Object> {
        static final Self INSTANCE = new Self();

        @Override public String toString() { return ""; }
        @Override public int compareTo(Object o) { return 0; }
    }

    /** get()은 Self, getOrDefault(k, default)는 기본값을 돌려주는 조회용 맵 스텁. */
    static final class LookupMap extends HashMap<Object, Object> {
        @Override public Object get(Object key) { return Self.INSTANCE; }
        @Override public Object getOrDefault(Object key, Object defaultValue) { return defaultValue; }
        @Override public boolean containsKey(Object key) { return false; }
    }

    /** 멤버 이름을 보고 타입에 맞는 스텁 값을 고른다. */
    private static Object memberValue(String member) {
        String m = member.toLowerCase(Locale.ROOT);
        if (isBooleanish(member, m)) {
            return Boolean.FALSE;
        }
        if (isDateish(m)) {
            return FIXED_DATE_TIME;
        }
        if (isNumberish(m)) {
            return 0;
        }
        return Self.INSTANCE;
    }

    private static boolean isBooleanish(String raw, String lower) {
        if (raw.startsWith("is") && raw.length() > 2 && Character.isUpperCase(raw.charAt(2))) {
            return true;
        }
        if ((raw.startsWith("has") || raw.startsWith("can"))
                && raw.length() > 3 && Character.isUpperCase(raw.charAt(3))) {
            return true;
        }
        return BOOLEAN_MEMBER_NAMES.contains(lower);
    }

    private static boolean isDateish(String lower) {
        return lower.endsWith("at")
                || lower.endsWith("date")
                || lower.endsWith("until")
                || lower.endsWith("time")
                || lower.equals("timestamp");
    }

    private static boolean isNumberish(String lower) {
        return lower.equals("id") || lower.endsWith("id")
                || lower.contains("count") || lower.contains("size") || lower.contains("total")
                || lower.contains("amount") || lower.contains("point") || lower.contains("rating")
                || lower.contains("index") || lower.contains("percent") || lower.contains("rank")
                || lower.contains("number") || lower.contains("length")
                || lower.contains("latitude") || lower.contains("longitude")
                || lower.equals("dau") || lower.equals("wau") || lower.equals("mau")
                || lower.equals("page") || lower.equals("year")
                || lower.equals("month") || lower.equals("day");
    }

    /** Self 의 임의 프로퍼티 접근을 멤버 이름 기반으로 해석. */
    static final class SelfPropertyAccessor implements PropertyAccessor {
        @Override public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[] {Self.class};
        }
        @Override public boolean canRead(EvaluationContext ctx, Object target, String name) {
            return target instanceof Self;
        }
        @Override public TypedValue read(EvaluationContext ctx, Object target, String name) {
            return new TypedValue(memberValue(name));
        }
        @Override public boolean canWrite(EvaluationContext ctx, Object target, String name) {
            return false;
        }
        @Override public void write(EvaluationContext ctx, Object target, String name, Object v) {
            // no-op: 템플릿은 쓰기 안 함
        }
    }

    /** Self 에 없는 메서드 호출(예: enum name(), record 접근자)을 멤버 이름 기반으로 해석. */
    static final class SelfMethodResolver implements MethodResolver {
        @Override public MethodExecutor resolve(EvaluationContext ctx, Object target,
                String name, List<TypeDescriptor> argumentTypes) {
            if (!(target instanceof Self)) {
                return null;
            }
            return (context, tgt, args) -> new TypedValue(memberValue(name));
        }
    }
}
