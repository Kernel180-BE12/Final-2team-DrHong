package com.jober.final2teamdrhong.service.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis를 사용하는 저장소 구현체입니다.
 * 빠른 속도와 TTL(만료 시간) 자동 처리 기능이 장점입니다.
 */
@Component("redisStorage") // 이 구현체의 이름을 "redisStorage"로 지정
@Profile("!redis-fallback-test") // Redis 폴백 테스트가 아닐 때만 활성화
public class RedisVerificationStorage implements VerificationStorage {

    private final StringRedisTemplate redisTemplate;
    
    public RedisVerificationStorage(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5); // 5분 유효

    @Override
    public void save(String key, String value) {
        // Redis에 값을 저장할 때, 5분의 만료 시간을 함께 설정합니다.
        redisTemplate.opsForValue().set(key, value, CODE_EXPIRATION);
    }

    @Override
    public Optional<String> find(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    /**
     * Redis에서 원자적 검증 및 삭제 (일회성 검증)
     * Lua 스크립트를 사용하여 레이스 컨디션 방지
     */
    @Override
    public boolean validateAndDelete(String key, String expectedValue) {
        // Lua 스크립트: 값을 비교하고 일치하면 삭제
        String luaScript = """
            local value = redis.call('GET', KEYS[1])
            if value == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            else
                return 0
            end
            """;
        
        Long result = redisTemplate.execute(
            (connection) -> connection.eval(
                luaScript.getBytes(),
                org.springframework.data.redis.connection.ReturnType.INTEGER,
                1,
                key.getBytes(),
                expectedValue.getBytes()
            ),
            true
        );
        
        return result != null && result == 1L;
    }
}
