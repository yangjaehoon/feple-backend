package com.feple.feple_backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
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
    void 뷰_컨트롤러_NoSuchElement는_HTML_에러_페이지_404_메시지_노출() throws Exception {
        mockMvc.perform(get("/admin/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW))
                .andExpect(model().attribute("errorMessage", "그런 항목 없음"))
                .andExpect(model().attribute("statusCode", 404));
    }

    @Test
    void 뷰_컨트롤러_잘못된_파라미터는_HTML_에러_페이지_400() throws Exception {
        mockMvc.perform(get("/admin/test/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name(AdminExceptionAdvice.ERROR_VIEW));
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
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/admin/access-denied"));
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
            throw new NoSuchElementException("그런 항목 없음");
        }

        @GetMapping("/bad")
        String bad() {
            throw new IllegalArgumentException("잘못된 파라미터");
        }

        @GetMapping("/denied")
        String denied() {
            throw new AccessDeniedException("권한 없음");
        }
    }

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
