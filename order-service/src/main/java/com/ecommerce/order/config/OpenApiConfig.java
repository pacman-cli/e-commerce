package com.ecommerce.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI orderServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Order Service API")
                                                .description("Manages order processing, retrieval, and history.")
                                                .version("1.0.0"));
        }
}
