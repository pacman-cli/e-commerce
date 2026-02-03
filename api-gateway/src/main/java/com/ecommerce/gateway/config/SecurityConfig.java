package com.ecommerce.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security configuration for API Gateway.
 * Implements rate limiting, security headers, and request validation.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Value("${security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${security.max-request-size:1048576}") // 1MB default
    private long maxRequestSize;

    /**
     * Rate limiting filter using the token bucket algorithm.
     * Limits requests per client IP.
     */
    @Bean
    public WebFilter rateLimitingFilter() {
        return new RateLimitingFilter(requestsPerMinute);
    }

    /**
     * Security headers filter.
     * Adds security headers to all responses.
     */
    @Bean
    public WebFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    /**
     * Request validation filter.
     * Validates request size and content.
     */
    @Bean
    public WebFilter requestValidationFilter() {
        return new RequestValidationFilter(maxRequestSize);
    }

    /**
     * Correlation ID filter.
     * Generates/Extracts correlation ID for request tracing.
     */
    @Bean
    public WebFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * Rate limiting implementation using in-memory token bucket.
     * For production, replace with Redis-based distributed rate limiting.
     */
    @Slf4j
    public static class RateLimitingFilter implements WebFilter {
        
        private final int maxRequests;
        private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
        
        public RateLimitingFilter(int maxRequests) {
            this.maxRequests = maxRequests;
            // Reset counters every minute
            Thread resetThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(60000);
                        requestCounts.clear();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            resetThread.setDaemon(true);
            resetThread.start();
        }

        /**
         * Filters requests, enforcing rate limits based on IP/path
         */
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String clientIp = getClientIp(exchange);
            String path = exchange.getRequest().getPath().value();
            
            // Skip rate limiting for health checks
            if (path.contains("/actuator/health")) {
                return chain.filter(exchange);
            }
            
            // Different limits for different endpoints
            int limit = getLimitForPath(path);
            
            AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
            int current = count.incrementAndGet();
            
            if (current > limit) {
                log.warn("Rate limit exceeded for IP: {}, path: {}, count: {}", clientIp, path, current);
                return rejectRequest(exchange, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            }
            
            return chain.filter(exchange);
        }
        
        private int getLimitForPath(String path) {
            if (path.contains("/login") || path.contains("/register")) {
                return 5; // Stricter for auth endpoints
            } else if (path.contains("/orders")) {
                return maxRequests;
            }
            return maxRequests * 2; // More lenient for other endpoints
        }
        
        /**
         * Extracts client IP from headers or remote address
         */
        private String getClientIp(ServerWebExchange exchange) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            
            String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return realIp;
            }
            
            return exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        }
        
        private Mono<Void> rejectRequest(ServerWebExchange exchange, HttpStatus status, String message) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            String body = String.format(
                "{\"error\":\"%s\",\"timestamp\":\"%s\",\"retryAfter\":60}",
                message,
                Instant.now().toString()
            );
            
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * Security headers filter implementation.
     */
    @Slf4j
    public static class SecurityHeadersFilter implements WebFilter {
        
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
                
                // Security headers
                response.getHeaders().set("X-Content-Type-Options", "nosniff");
                response.getHeaders().set("X-Frame-Options", "DENY");
                response.getHeaders().set("X-XSS-Protection", "1; mode=block");
                response.getHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                response.getHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'");
                response.getHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
                response.getHeaders().set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
                
                // Remove headers that leak information
                response.getHeaders().remove("Server");
                response.getHeaders().remove("X-Powered-By");
            }));
        }
    }

    /**
     * Request validation filter.
     */
    @Slf4j
    public static class RequestValidationFilter implements WebFilter {
        
        private final long maxRequestSize;
        
        public RequestValidationFilter(long maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            // Check request size
            String contentLength = exchange.getRequest().getHeaders().getFirst("Content-Length");
            if (contentLength != null) {
                try {
                    long length = Long.parseLong(contentLength);
                    if (length > maxRequestSize) {
                        log.warn("Request too large: {} bytes from {}", 
                            length, 
                            exchange.getRequest().getRemoteAddress()
                        );
                        return rejectRequest(exchange, HttpStatus.PAYLOAD_TOO_LARGE, 
                            "Request size exceeds maximum allowed size of " + maxRequestSize + " bytes");
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid Content-Length header: {}", contentLength);
                }
            }
            
            // Validate content type for POST/PUT
            String method = exchange.getRequest().getMethod().name();
            if (("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                String contentType = exchange.getRequest().getHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.contains("application/json")) {
                    return rejectRequest(exchange, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Content-Type must be application/json");
                }
            }
            
            return chain.filter(exchange);
        }
        
        private Mono<Void> rejectRequest(ServerWebExchange exchange, HttpStatus status, String message) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            String body = String.format(
                "{\"error\":\"%s\",\"timestamp\":\"%s\"}",
                message,
                Instant.now().toString()
            );
            
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * Correlation ID filter for request tracing.
     */
    @Slf4j
    public static class CorrelationIdFilter implements WebFilter {
        
        private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
        
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            final String finalCorrelationId = correlationId;
            
            // Add to response
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
            
            // Add to MDC for logging
            return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CORRELATION_ID_HEADER, finalCorrelationId));
        }
    }
}
