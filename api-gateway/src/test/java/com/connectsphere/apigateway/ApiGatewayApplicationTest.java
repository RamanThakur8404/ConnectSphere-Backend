package com.connectsphere.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

class ApiGatewayApplicationTest {

	@Test
	void mainDelegatesToSpringApplicationRun() {
		String[] args = { "--spring.profiles.active=test" };

		try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
			ApiGatewayApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(ApiGatewayApplication.class, args), times(1));
		}
	}

	@Test
	void objectMapperBeanReturnsMapper() {
		ObjectMapper mapper = new ApiGatewayApplication().objectMapper();

		assertThat(mapper).isNotNull();
	}
}
