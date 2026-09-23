package com.feple.feple_backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API 문서 경로의 공개 여부를 고정한다.
 *
 * <p>2026-09-23 운영에서 /v3/api-docs가 인증 없이 200으로 열려 있었다(엔드포인트 121개·DTO 80개
 * 노출). 원인은 공개 여부 게이트가 {@code springdoc.api-docs.enabled}였던 것 — 배포 시크릿
 * (APPLICATION_LOCAL_YAML)에 개발자의 로컬 설정이 통째로 복사돼 들어가면서 문서 생성과 공개
 * 접근이 한꺼번에 켜졌다. 게이트를 {@code app.swagger.public-access}로 분리해, 시크릿에
 * springdoc 설정이 남아 있어도 기본값에서는 닫히도록 했다.
 */
class SwaggerAccessSecurityTest {

    // springdoc만 켜고 공개 게이트는 설정하지 않은 상태 = 운영에서 문서가 열려 있던 조합 그대로.
    @Nested
    @SpringBootTest(properties = {
            "springdoc.api-docs.enabled=true",
            "springdoc.swagger-ui.enabled=true",
            // application-test.yml이 이미 false로 두지만, 이 테스트의 전제를 한눈에 보이게 명시한다
            "app.swagger.public-access=false"
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("springdoc은 켜져 있지만 app.swagger.public-access를 선언하지 않은 경우")
    class ClosedByDefault {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        S3Template s3Template;

        @Test
        @DisplayName("springdoc이 켜져 있어도 /v3/api-docs는 인증을 요구한다")
        void apiDocs_인증요구() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("/swagger-ui도 인증을 요구한다")
        void swaggerUi_인증요구() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "app.swagger.public-access=true",
            "springdoc.api-docs.enabled=true",
            "springdoc.swagger-ui.enabled=true"
    })
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("명시적으로 공개를 선언한 경우(로컬 개발)")
    class OpenWhenDeclared {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        S3Template s3Template;

        @Test
        @DisplayName("/v3/api-docs가 인증 없이 열린다")
        void apiDocs_공개() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }
}
