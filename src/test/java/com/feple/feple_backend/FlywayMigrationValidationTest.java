package com.feple.feple_backend;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 나머지 테스트는 flyway.enabled=false + ddl-auto=create-drop(H2)라 Flyway 마이그레이션이
// 실제로 적용된 적이 없어, 엔티티만 추가하고 마이그레이션을 빠뜨려도 CI가 잡지 못했다
// (V40__create_timetable_entry_member.sql 누락 사고). 실제 MySQL에 전체 마이그레이션을
// 적용한 뒤 ddl-auto=validate로 모든 엔티티 매핑을 대조해 이 종류의 누락을 잡는다.
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FlywayMigrationValidationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideSchemaManagement(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Hibernate의 자동 dialect 추론이 MySQL 커넥션에서 information_schema.SEQUENCES를
        // 조회하는 경로로 빠지는 경우가 있어(MySQL은 시퀀스 미지원) 명시적으로 고정한다.
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @MockBean
    S3Template s3Template;

    @Test
    void 플라이웨이_마이그레이션_전체가_엔티티_매핑과_일치한다() {
        // 컨텍스트 로딩 자체가 검증: Flyway가 실제 MySQL에 모든 마이그레이션을 에러 없이
        // 적용하고, Hibernate가 ddl-auto=validate로 모든 @Entity를 그 스키마와 대조한다.
    }
}
