package com.ecommerce.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI paymentServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Payment Service API")
                                                .description("Handles payment processing and status checks.")
                                                .version("1.0.0"));
        }
}
