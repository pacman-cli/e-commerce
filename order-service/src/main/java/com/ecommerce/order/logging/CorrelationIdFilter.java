package com.ecommerce.order.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Filter that manages correlation IDs and MDC (Mapped Diagnostic Context) for logging.
 * 
 * This filter:
 * 1. Extracts or generates correlation ID from request headers
 * 2. Adds correlation ID to MDC for logging
 * 3. Adds correlation ID to response headers
 * 4. Logs request/response details
 * 5. Cleans up MDC after request completion
 * 
 * The correlation ID is used to trace a request across multiple services.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // Execute before other filters
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String REQUEST_PATH_MDC_KEY = "requestPath";
    public static final String REQUEST_METHOD_MDC_KEY = "requestMethod";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String correlationId = extractOrGenerateCorrelationId(request);
        String userId = extractUserId(request);
        
        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(REQUEST_PATH_MDC_KEY, request.getRequestURI());
        MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());
        
        if (userId != null) {
            MDC.put(USER_ID_MDC_KEY, userId);
        }
        
        // Add to response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        // Wrap request/response to capture content for logging
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        Instant start = Instant.now();
        
        try {
            logRequest(wrappedRequest);
            
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
            Duration duration = Duration.between(start, Instant.now());
            logResponse(wrappedResponse, duration);
            
        } finally {
            // Copy content to response before cleaning up
            wrappedResponse.copyBodyToResponse();
            
            // Clean up MDC
            MDC.clear();
        }
    }
    
    /**
     * Extract correlation ID from request header or generate a new one.
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Received correlation ID: {}", correlationId);
        }
        
        return correlationId;
    }
    
    /**
     * Extract user ID from request headers (set by API Gateway).
     */
    private String extractUserId(HttpServletRequest request) {
        return request.getHeader("X-User-Id");
    }
    
    /**
     * Generate a new correlation ID.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Log incoming request details.
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming request: {} {} - CorrelationId: {}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get(CORRELATION_ID_MDC_KEY));
        }
    }
    
    /**
     * Log outgoing response details.
     */
    private void logResponse(ContentCachingResponseWrapper response, Duration duration) {
        int status = response.getStatus();
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        
        if (status >= 500) {
            log.error("Response: {} - Duration: {}ms - CorrelationId: {}",
                status, duration.toMillis(), correlationId);
        } else if (status >= 400) {
            log.warn("Response: {} - Duration: {}ms - CorrelationId: {}",
                status, duration.toMillis(), correlationId);
        } else {
            log.info("Response: {} - Duration: {}ms - CorrelationId: {}",
                status, duration.toMillis(), correlationId);
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't filter health checks and actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || 
               path.startsWith("/actuator/info") ||
               path.startsWith("/actuator/prometheus");
    }
}
