package com.feple.feple_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)

public class FepleBackendApplication {

	public static void main(String[] args) {
		// 배포 환경(EC2)의 OS 기본 타임존이 UTC일 수 있어, 코드 전반의 LocalDate.now()/
		// LocalDateTime.now()가 KST 자정~오전9시 사이 "오늘"을 하루 전으로 계산하는 것을 방지
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(FepleBackendApplication.class, args);
	}

}
