package com.ecommerce.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI notificationServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Notification Service API")
                                                .description("Handles email notifications and test triggers.")
                                                .version("1.0.0"));
        }
}
