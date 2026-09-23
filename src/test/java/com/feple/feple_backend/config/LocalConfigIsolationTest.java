package com.feple.feple_backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.s3.S3Template;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 테스트 환경이 {@code application-local.yaml}을 로드하지 않는지 고정한다.
 *
 * <p>이 파일은 개발자 PC에서는 개인 설정이고, 배포 시에는 {@code deploy.yml}이 운영 시크릿
 * (APPLICATION_LOCAL_YAML)을 같은 경로에 쓴 뒤 테스트를 돌린다. 예전에는
 * {@code application.yml}의 {@code spring.config.import}가 무조건 실행돼 양쪽 모두 테스트로
 * 흘러들어왔다 — {@code application-test.yml}의 {@code spring.config.import: ""}로는 막히지
 * 않는다(import는 값이 아니라 파일을 읽는 도중 즉시 실행되는 지시문이라, 나중에 덮어써도
 * 이미 로드된 값은 남는다). 그래서 {@code application-test.yml}에 같은 키가 없는 설정은
 * 전부 새어들어왔고, 테스트 결과가 각자의 로컬 파일 내용에 좌우됐다.
 *
 * <p>지금은 import가 {@code spring.config.activate.on-profile: "!test"} 문서로 분리돼 있다.
 * 값이 아니라 PropertySource 자체를 확인하므로 로컬 파일이 있든 없든 동일하게 동작한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class LocalConfigIsolationTest {

    @Autowired
    ConfigurableEnvironment environment;

    @MockitoBean
    S3Template s3Template;

    @Test
    @DisplayName("test 프로필에서는 application-local.yaml이 PropertySource로 등록되지 않는다")
    void 로컬설정_미로드() {
        List<String> sourceNames = environment.getPropertySources().stream()
                .map(PropertySource::getName)
                .toList();

        assertThat(sourceNames)
                .as("application-local.yaml이 로드되면 개발자 PC의 개인 설정과 배포 시크릿의 "
                        + "운영 값이 테스트로 새어들어온다. 등록된 PropertySource: %s", sourceNames)
                .noneMatch(name -> name.contains("application-local"));
    }
}
