package com.ecommerce.order.api.common;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Request Mapping Handler for API Versioning
 *
 * Integrates with Spring's request mapping to support versioned controllers.
 * Must be registered in the application configuration.
 *
 * This handler checks for @ApiVersion annotation on controllers and applies
 * version matching logic.
 */
@Slf4j
public class ApiVersionRequestMappingHandler extends RequestMappingHandlerMapping {

    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return createCondition(apiVersion);
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return createCondition(apiVersion);
    }

    private RequestCondition<?> createCondition(ApiVersion apiVersion) {
        if (apiVersion != null && !apiVersion.value().isEmpty()) {
            return new ApiVersionCondition(apiVersion.value());
        }
        return null;
    }
}
