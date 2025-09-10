package com.jober.final2teamdrhong.config;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.host")
@Profile("!redis-fallback-test") // redis-fallback-test 프로파일이 아닐 때만 활성화
public class RedisConfig {
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        
        // Key serializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return redisTemplate;
    }
    
    @Bean
    public LettuceBasedProxyManager<byte[]> lettuceBasedProxyManager() {
        try {
            // Redis URL 동적 생성
            String redisUrl = buildRedisUrl();
            log.info("Redis 연결 시도: host={}, port={}", redisHost, redisPort);
            
            RedisClient redisClient = RedisClient.create(redisUrl);
            
            return LettuceBasedProxyManager.builderFor(redisClient)
                    .build();
        } catch (Exception e) {
            log.error("Redis 연결 실패: host={}, port={}, error={}", redisHost, redisPort, e.getMessage());
            throw new IllegalStateException("Redis 연결에 실패했습니다. Redis 서버 상태를 확인해주세요.", e);
        }
    }
    
    /**
     * application.properties 설정을 기반으로 Redis URL 생성
     */
    private String buildRedisUrl() {
        StringBuilder urlBuilder = new StringBuilder("redis://");
        
        // 비밀번호가 있는 경우 추가
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            urlBuilder.append(":").append(redisPassword).append("@");
        }
        
        urlBuilder.append(redisHost).append(":").append(redisPort);
        
        return urlBuilder.toString();
    }
    
    /**
     * Redis 캐시 매니저 설정
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)) // 기본 TTL 30분
                    .disableCachingNullValues() // null 값 캐싱 비활성화
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
            
            log.info("Redis 캐시 매니저 초기화 완료");
            
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(cacheConfig)
                    .build();
                    
        } catch (Exception e) {
            log.error("Redis 캐시 매니저 초기화 실패: {}", e.getMessage());
            throw new IllegalStateException("Redis 캐시 설정에 실패했습니다.", e);
        }
    }
}