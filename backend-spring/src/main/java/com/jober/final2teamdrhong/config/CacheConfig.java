package com.jober.final2teamdrhong.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 캐시 설정
 * 
 * Redis가 사용 가능한 경우 Redis 캐시 사용, 
 * 그렇지 않으면 인메모리 캐시 사용
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis 캐시 매니저 (Redis 연결 가능 시)
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "fallbackCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 30분 TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }

    /**
     * 인메모리 캐시 매니저 (Redis 비활성화 시 폴백)
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public CacheManager fallbackCacheManager() {
        return new ConcurrentMapCacheManager("userInfo");
    }
}