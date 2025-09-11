package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.JwtConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BlacklistService 기본 테스트
 * 서비스 주입 및 기본 동작 확인
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private BlacklistService blacklistService;

    @Test
    @DisplayName("BlacklistService가 정상적으로 주입되는지 확인")
    void shouldInjectBlacklistService() {
        // when & then
        assertThat(blacklistService).isNotNull();
    }

    @Test
    @DisplayName("블랙리스트 확인 기능 기본 테스트")
    void shouldCheckTokenBlacklist() {
        // given
        String token = "test.token";
        
        // when
        boolean result = blacklistService.isTokenBlacklisted(token);

        // then - 유효하지 않은 토큰은 보안상 블랙리스트로 처리
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("null 토큰에 대한 블랙리스트 확인")
    void shouldHandleNullToken() {
        // when
        boolean result = blacklistService.isTokenBlacklisted(null);

        // then - null 토큰은 보안상 블랙리스트로 처리
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("빈 문자열 토큰에 대한 블랙리스트 확인")
    void shouldHandleEmptyToken() {
        // when
        boolean result = blacklistService.isTokenBlacklisted("");

        // then - 빈 토큰은 보안상 블랙리스트로 처리
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis 템플릿이 정상적으로 주입되는지 확인")
    void shouldInjectRedisTemplate() {
        // when & then
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    @DisplayName("JWT 설정이 정상적으로 주입되는지 확인")
    void shouldInjectJwtConfig() {
        // when & then
        assertThat(jwtConfig).isNotNull();
    }

    @Test
    @DisplayName("기본 설정값 확인")
    void shouldHaveDefaultConfiguration() {
        // when & then - 기본 설정으로 BlacklistService가 생성되었는지 확인
        assertThat(blacklistService).isNotNull();
        assertThat(redisTemplate).isNotNull();
        assertThat(jwtConfig).isNotNull();
    }
}