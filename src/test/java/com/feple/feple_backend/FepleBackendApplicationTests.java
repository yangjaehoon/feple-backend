package com.feple.feple_backend;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FepleBackendApplicationTests {

	@MockitoBean
	S3Template s3Template;

	@Test
	void contextLoads() {
	}

}
