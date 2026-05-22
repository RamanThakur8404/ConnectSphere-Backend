package com.connectsphere.search_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@SpringBootTest
@ActiveProfiles("test")
class SearchServiceApplicationTests {

    @MockBean
    private ConnectionFactory connectionFactory;

	@Test
	void contextLoads() {
	}
}
