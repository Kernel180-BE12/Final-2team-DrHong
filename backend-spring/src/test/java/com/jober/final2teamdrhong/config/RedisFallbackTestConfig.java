package com.jober.final2teamdrhong.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Redis 폴백 테스트를 위한 더미 Redis 설정
 * Redis가 비활성화된 상태에서도 RedisTemplate 관련 의존성 오류를 방지
 */
@TestConfiguration
@Profile("redis-fallback-test")
public class RedisFallbackTestConfig {
    
    /**
     * 더미 RedisConnectionFactory 제공
     * 실제로는 사용되지 않지만 Bean 의존성 해결을 위해 필요
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory() {
        // 실제 연결은 하지 않는 더미 factory
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setValidateConnection(false);
        return factory;
    }
    
    /**
     * 더미 RedisTemplate 제공
     * 실제로는 사용되지 않지만 Bean 의존성 해결을 위해 필요
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        // afterPropertiesSet() 호출하지 않음 - 실제 초기화 방지
        return template;
    }
}