package com.connectsphere.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;

@SpringBootTest(properties = {
		"JWT_SECRET=c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0c2VjcmV0",
		"eureka.client.enabled=false"
})
class ApiGatewayContextTest {

	@Autowired
	private List<GatewayFilterFactory<?>> gatewayFilterFactories;

	@Test
	void authenticationFilterFactoryIsRegistered() {
		assertThat(gatewayFilterFactories)
				.anySatisfy(factory -> assertThat(factory.name()).isEqualTo("AuthenticationFilter"));
	}
}
