package com.ecommerce.order.api.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * API Version Condition for Content Negotiation
 * 
 * Supports three versioning strategies:
 * 1. URL Path (/api/v1/orders, /api/v2/orders)
 * 2. Request Header (X-API-Version: v1)
 * 3. Content-Type (Accept: application/vnd.ecommerce.v1+json)
 * 
 * Priority (first match wins):
 * 1. URL Path version
 * 2. X-API-Version header
 * 3. Content-Type version
 * 4. Default to v1 if no version specified
 * 
 * Example Usage:
 * ```java
 * @ApiVersion("v1")
 * @RestController
 * @RequestMapping("/api/orders")
 * public class OrderControllerV1 { }
 * 
 * @ApiVersion("v2")
 * @RestController
 * @RequestMapping("/api/orders")
 * public class OrderControllerV2 { }
 * ```
 */
public class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {
    
    private final String version;
    private static final String HEADER_NAME = "X-API-Version";
    private static final String CONTENT_TYPE_PREFIX = "application/vnd.ecommerce.";
    private static final String DEFAULT_VERSION = "v1";
    
    public ApiVersionCondition(String version) {
        this.version = version;
    }
    
    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        // Use the most specific version
        return new ApiVersionCondition(other.version);
    }
    
    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
        String requestedVersion = extractVersion(request);
        
        if (version.equals(requestedVersion)) {
            return this;
        }
        
        return null;
    }
    
    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
        // Prefer more specific versions
        return version.compareTo(other.version);
    }
    
    /**
     * Extract version from request using multiple strategies
     */
    private String extractVersion(HttpServletRequest request) {
        // 1. Check URL path (e.g., /api/v1/orders)
        String pathVersion = extractVersionFromPath(request.getRequestURI());
        if (pathVersion != null) {
            return pathVersion;
        }
        
        // 2. Check header
        String headerVersion = request.getHeader(HEADER_NAME);
        if (headerVersion != null && !headerVersion.isEmpty()) {
            return headerVersion.toLowerCase();
        }
        
        // 3. Check Accept header content type
        String contentTypeVersion = extractVersionFromContentType(
            request.getHeader("Accept")
        );
        if (contentTypeVersion != null) {
            return contentTypeVersion;
        }
        
        // 4. Default to v1
        return DEFAULT_VERSION;
    }
    
    /**
     * Extract version from URL path
     * Matches patterns like /api/v1/, /api/v2/, /v1/, /v2/
     */
    private String extractVersionFromPath(String uri) {
        if (uri == null) return null;
        
        // Match /v1/, /v2/, /api/v1/, etc.
        String[] parts = uri.split("/");
        for (String part : parts) {
            if (part.matches("v\\d+")) {
                return part.toLowerCase();
            }
        }
        return null;
    }
    
    /**
     * Extract version from Accept header
     * Accept: application/vnd.ecommerce.v1+json
     */
    private String extractVersionFromContentType(String acceptHeader) {
        if (acceptHeader == null) return null;
        
        for (String type : acceptHeader.split(",")) {
            type = type.trim();
            if (type.startsWith(CONTENT_TYPE_PREFIX)) {
                // Extract version from "application/vnd.ecommerce.v1+json"
                int start = CONTENT_TYPE_PREFIX.length();
                int end = type.indexOf("+", start);
                if (end == -1) {
                    end = type.indexOf(";", start);
                }
                if (end == -1) {
                    end = type.length();
                }
                return type.substring(start, end).toLowerCase();
            }
        }
        return null;
    }
    
    public String getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return "ApiVersionCondition{version='" + version + "'}";
    }
}
