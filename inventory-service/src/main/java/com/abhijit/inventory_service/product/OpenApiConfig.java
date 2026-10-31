package com.abhijit.inventory_service.product;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
	
	@Bean
    public OpenAPI inventoryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Inventory Service API")
                .version("1.0")
                .description("Product catalogue for the mini order system"));
    }

}
