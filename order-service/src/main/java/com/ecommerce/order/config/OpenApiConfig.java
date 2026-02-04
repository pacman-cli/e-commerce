package com.ecommerce.order.config;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.order.exception.ValidationErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;

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

        /**
         * Customizer to explicitly add ValidationErrorResponse to OpenAPI schemas.
         * This fixes the "does not exist in document" error in Swagger UI.
         */
        @Bean
        public OpenApiCustomizer schemaCustomizer() {
                return openApi -> {
                        // Get resolved schemas for ValidationErrorResponse
                        Map<String, Schema> schemas = ModelConverters.getInstance()
                                        .read(ValidationErrorResponse.class);

                        // Add each schema to components
                        schemas.forEach((name, schema) -> {
                                if (openApi.getComponents().getSchemas() == null) {
                                        openApi.getComponents().setSchemas(new java.util.HashMap<>());
                                }
                                openApi.getComponents().getSchemas().put(name, schema);
                        });
                };
        }
}
