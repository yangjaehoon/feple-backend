package com.feple.feple_backend;

import com.feple.feple_backend.global.KoreaClock;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)

public class FepleBackendApplication {

	static {
		// 배포 환경(EC2)의 OS 기본 타임존이 UTC라, 코드 전반의 LocalDate.now()/LocalDateTime.now()와
		// @CreationTimestamp, zone 미지정 @Scheduled 크론이 UTC로 동작하는 것을 막는다.
		// main() 본문이 아니라 static 블록에 두는 이유: @SpringBootTest는 main()을 타지 않고
		// 이 클래스를 설정 소스로 로드하므로, 클래스 초기화 시점에 실행돼야 통합 테스트도 KST로 맞춰진다.
		// (main()을 아예 타지 않는 순수 단위 테스트는 build.gradle test 태스크의 user.timezone 설정으로 커버.)
		TimeZone.setDefault(TimeZone.getTimeZone(KoreaClock.ZONE_ID));
	}

	public static void main(String[] args) {
		SpringApplication.run(FepleBackendApplication.class, args);
	}

}
