package com.ecommerce.order.api.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Web Configuration for API Versioning
 * 
 * Registers the custom request mapping handler that supports API versioning.
 */
@Configuration
public class ApiVersionWebConfig extends WebMvcConfigurationSupport {
    
    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandler();
    }
}
