package com.connectsphere.eurekaserver;

import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class EurekaServerApplicationTest {

	@Test
	void constructorCreatesApplicationInstance() {
		assertThat(new EurekaServerApplication()).isNotNull();
	}

	@Test
	void mainDelegatesToSpringApplicationRun() {
		String[] args = { "--spring.profiles.active=test" };

		try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
			EurekaServerApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(EurekaServerApplication.class, args), times(1));
		}
	}
}
