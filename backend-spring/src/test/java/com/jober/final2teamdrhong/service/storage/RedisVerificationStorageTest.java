package com.jober.final2teamdrhong.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RedisVerificationStorage 테스트
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RedisVerificationStorageTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisVerificationStorage redisVerificationStorage;

    private final String testKey = "email_verification:test@example.com";
    private final String testCode = "123456";

    @BeforeEach
    void setUp() {
        redisVerificationStorage = new RedisVerificationStorage(redisTemplate);
    }

    @Test
    @DisplayName("인증 코드 저장")
    void shouldSaveVerificationCode() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        
        // when
        redisVerificationStorage.save(testKey, testCode);

        // then
        verify(valueOperations).set(eq(testKey), eq(testCode), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("인증 코드 조회")
    void shouldRetrieveVerificationCode() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(testKey)).willReturn(testCode);

        // when
        Optional<String> result = redisVerificationStorage.find(testKey);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testCode);
        verify(valueOperations).get(testKey);
    }

    @Test
    @DisplayName("존재하지 않는 인증 코드 조회")
    void shouldReturnEmptyWhenCodeNotExists() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(testKey)).willReturn(null);

        // when
        Optional<String> result = redisVerificationStorage.find(testKey);

        // then
        assertThat(result).isEmpty();
        verify(valueOperations).get(testKey);
    }

    @Test
    @DisplayName("인증 코드 삭제")
    void shouldDeleteVerificationCode() {
        // when
        redisVerificationStorage.delete(testKey);

        // then
        verify(redisTemplate).delete(testKey);
    }

    @Test
    @DisplayName("Redis 템플릿이 정상적으로 주입되는지 확인")
    void shouldInjectRedisTemplate() {
        // when & then
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    @DisplayName("기본 설정값 확인")
    void shouldHaveDefaultConfiguration() {
        // when & then - 기본 설정으로 RedisVerificationStorage가 생성되었는지 확인
        assertThat(redisVerificationStorage).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }
}