package com.in28minutes.springboot.learn_spring_boot.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
	
	@Bean
    public PriceFormatter priceFormatter() {
        return new PriceFormatter("INR");
    }

}
