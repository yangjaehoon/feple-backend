package com.feple.feple_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync

public class FepleBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FepleBackendApplication.class, args);
	}

}
