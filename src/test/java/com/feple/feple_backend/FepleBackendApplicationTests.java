package com.feple.feple_backend;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FepleBackendApplicationTests {

	@MockBean
	S3Template s3Template;

	@Test
	void contextLoads() {
	}

}
