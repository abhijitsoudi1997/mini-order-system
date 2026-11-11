package com.abhijit.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.tracing.sampling.probability=0.0"})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

    @org.springframework.beans.factory.annotation.Autowired
    private io.micrometer.tracing.Tracer tracer;

    @org.junit.jupiter.api.Test
    void aTracerIsConfigured() {
        org.assertj.core.api.Assertions.assertThat(tracer).isNotNull();
    }
}
