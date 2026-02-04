package com.ecommerce.order.api.common;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Web Configuration for API Versioning
 *
 * Registers the custom request mapping handler that supports API versioning.
 * Uses WebMvcRegistrations to avoid disabling Spring Boot's auto-configuration.
 */
@Configuration
public class ApiVersionWebConfig implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandler();
    }
}
