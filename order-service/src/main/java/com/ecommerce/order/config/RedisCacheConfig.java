package com.ecommerce.order.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis caching configuration for Order Service.
 * 
 * Provides:
 * - Redis connection factory with connection pooling
 * - Cache manager with TTL configuration
 * - JSON serialization for cached objects
 * - Multiple cache regions with different TTLs
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:2000}")
    private int redisTimeout;
    
    /**
     * Redis connection factory with connection pooling.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(redisTimeout))
            .build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    /**
     * Redis template for manual cache operations.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer with type info for polymorphism
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Primary cache manager with multiple cache configurations.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration: 5 minutes TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues()
            .prefixCacheNameWith("order-service:");
        
        // Short-lived cache: 1 minute (for frequently changing data)
        RedisCacheConfiguration shortLivedConfig = defaultConfig
            .entryTtl(Duration.ofMinutes(1))
            .prefixCacheNameWith("order-service:short:");
        
        // Long-lived cache: 1 hour (for relatively static data)
        RedisCacheConfiguration longLivedConfig = defaultConfig
            .entryTtl(Duration.ofHours(1))
            .prefixCacheNameWith("order-service:long:");
        
        // Order-specific cache: 10 minutes (balances freshness and performance)
        RedisCacheConfiguration orderConfig = defaultConfig
            .entryTtl(Duration.ofMinutes(10))
            .prefixCacheNameWith("order-service:orders:");
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("orders", orderConfig)
            .withCacheConfiguration("orderList", shortLivedConfig)
            .withCacheConfiguration("userStats", longLivedConfig)
            .withCacheConfiguration("productCache", longLivedConfig)
            .transactionAware()
            .build();
    }
    
    /**
     * Cache evict helper - can be used to clear caches programmatically.
     */
    @Bean
    public CacheEvictHelper cacheEvictHelper(CacheManager cacheManager) {
        return new CacheEvictHelper(cacheManager);
    }
    
    /**
     * Helper class for programmatic cache eviction.
     */
    public static class CacheEvictHelper {
        private final CacheManager cacheManager;
        
        public CacheEvictHelper(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }
        
        public void evictOrder(String orderId) {
            var cache = cacheManager.getCache("orders");
            if (cache != null) {
                cache.evict(orderId);
            }
        }
        
        public void evictUserOrders(String userId) {
            var cache = cacheManager.getCache("orderList");
            if (cache != null) {
                cache.evict(userId);
            }
        }
        
        public void clearAllCaches() {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }
}
