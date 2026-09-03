package com.feple.feple_backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.search.service.SearchService;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 비로그인 게스트 둘러보기(Apple 가이드라인 5.1.1(v))로 열어둔 화면들이 호출하는
 * 비계정 GET 엔드포인트가 인증 없이 접근 가능한지 검증한다. 여기서 401이 나면
 * 프론트의 DioClient가 "세션 만료"로 오인해 강제 로그아웃 정리(캐시 삭제 등)를
 * 태우므로, SecurityConfig permitAll 목록에서 빠지지 않도록 회귀를 고정한다.
 *
 * SearchService.search/getSuggestions는 MySQL 전용 FULLTEXT native query라 H2에서
 * 실행되지 않으므로 mock으로 대체하고, 필터 체인의 인가 판정만 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestApiAccessSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    S3Template s3Template;

    @MockitoBean
    SearchService searchService;

    @Test
    @DisplayName("게스트: 통합 검색은 인증 없이 접근 가능")
    void 게스트_통합검색_접근가능() throws Exception {
        mockMvc.perform(get("/search").param("keyword", "coldplay"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게스트: 검색 자동완성은 인증 없이 접근 가능")
    void 게스트_검색자동완성_접근가능() throws Exception {
        mockMvc.perform(get("/search/suggestions").param("keyword", "cold"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게스트: 내 인증 상태 조회는 인증 없이 접근 가능하고 NONE을 반환")
    void 게스트_인증상태_NONE반환() throws Exception {
        mockMvc.perform(get("/certifications/cert-state").param("festivalId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("NONE")));
    }

    @Test
    @DisplayName("게스트: 페스티벌 좋아요 여부 조회는 인증 없이 false를 반환")
    void 게스트_좋아요여부_false반환() throws Exception {
        mockMvc.perform(get("/festivals/1/liked"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("게스트: 알림 미읽음 개수는 계정 전용이라 401")
    void 게스트_알림개수_401() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }
}
