package com.feple.feple_backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

class AdminExceptionAdviceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HtmlController(), new JsonController())
                .setControllerAdvice(new AdminExceptionAdvice())
                .build();
    }

    @Test
    void 뷰_컨트롤러_예상못한_예외는_HTML_에러_페이지_500() throws Exception {
        mockMvc.perform(get("/admin/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("statusCode", 500));
    }

    @Test
    void 뷰_컨트롤러_ResourceNotFoundException은_404_메시지_그대로_노출() throws Exception {
        mockMvc.perform(get("/admin/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("errorMessage", "그런 항목 없음"))
                .andExpect(model().attribute("statusCode", 404));
    }

    @Test
    void 뷰_컨트롤러_순수_NoSuchElement는_404_일반_메시지로_마스킹() throws Exception {
        mockMvc.perform(get("/admin/test/missing-raw"))
                .andExpect(status().isNotFound())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("errorMessage", "요청한 항목을 찾을 수 없습니다."));
    }

    @Test
    void 뷰_컨트롤러_순수_IllegalArgument는_400_일반_메시지로_마스킹() throws Exception {
        mockMvc.perform(get("/admin/test/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("errorMessage",
                        "요청 값이 올바르지 않습니다. 입력을 확인하고 다시 시도해주세요."));
    }

    @Test
    void 뷰_컨트롤러_InvalidRequestException은_400_메시지_그대로_노출() throws Exception {
        mockMvc.perform(get("/admin/test/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("errorMessage", "이미 사용 중인 아이디입니다: admin"));
    }

    @Test
    void 뷰_컨트롤러_ModelAttribute_바인딩_실패는_500이_아니라_400_일반_메시지() throws Exception {
        mockMvc.perform(get("/admin/test/bind").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("errorMessage",
                        "요청 값이 올바르지 않습니다. 입력을 확인하고 다시 시도해주세요."));
    }

    @Test
    void ResponseBody_컨트롤러_예외는_JSON_으로_응답() throws Exception {
        mockMvc.perform(get("/admin/test/json-boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("SERVER_ERROR"));
    }

    @Test
    void 접근_거부는_500이_아니라_접근거부_화면으로_리다이렉트() throws Exception {
        mockMvc.perform(get("/admin/test/denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/access-denied"));
    }

    @Controller
    @RequestMapping("/admin/test")
    static class HtmlController {
        @GetMapping("/boom")
        String boom() {
            throw new RuntimeException("내부 예외 상세");
        }

        @GetMapping("/missing")
        String missing() {
            throw new ResourceNotFoundException("그런 항목 없음");
        }

        @GetMapping("/missing-raw")
        String missingRaw() {
            throw new NoSuchElementException("No value present");
        }

        @GetMapping("/bad")
        String bad() {
            throw new IllegalArgumentException("No enum constant Foo.BAR");
        }

        @GetMapping("/invalid")
        String invalid() {
            throw new InvalidRequestException("이미 사용 중인 아이디입니다: admin");
        }

        @GetMapping("/denied")
        String denied() {
            throw new AccessDeniedException("권한 없음");
        }

        @GetMapping("/bind")
        String bind(@ModelAttribute BindTestParams params) {
            return "ok";
        }
    }

    record BindTestParams(Integer page) {}

    @Controller
    @RequestMapping("/admin/test")
    static class JsonController {
        @GetMapping("/json-boom")
        @ResponseBody
        String jsonBoom() {
            throw new RuntimeException("내부 예외 상세");
        }
    }
}
