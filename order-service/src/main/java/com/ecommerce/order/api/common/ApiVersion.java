package com.ecommerce.order.api.common;

import java.lang.annotation.*;

/**
 * API Version Annotation
 * 
 * Marks controllers or methods with a specific API version.
 * Used by the versioning infrastructure to route requests correctly.
 * 
 * Example:
 * ```java
 * @ApiVersion("v1")
 * @RestController
 * @RequestMapping("/api/orders")
 * public class OrderControllerV1 {
 *     // Handles /api/v1/orders or /api/orders with X-API-Version: v1
 * }
 * 
 * @ApiVersion("v2")
 * @RestController
 * @RequestMapping("/api/orders")
 * public class OrderControllerV2 {
 *     // Handles /api/v2/orders or /api/orders with X-API-Version: v2
 * }
 * ```
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    /**
     * API Version string (e.g., "v1", "v2", "2024-01")
     */
    String value();
    
    /**
     * Whether this is the default version when no version is specified
     */
    boolean defaultVersion() default false;
    
    /**
     * Deprecation notice - if set, this version is deprecated
     */
    String deprecated() default "";
    
    /**
     * Sunset date - when this version will be removed
     */
    String sunset() default "";
}
