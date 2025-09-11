package com.jober.final2teamdrhong.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthProperties 설정 테스트
 * application.properties의 auth 설정이 올바르게 로드되는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "auth.security.min-response-time-ms=300",
    "auth.security.dummy-hash=test-dummy-hash",
    "auth.security.max-login-attempts=3",
    "auth.security.account-lock-duration-minutes=15",
    "auth.token.access-token-validity-seconds=1800",
    "auth.token.refresh-token-validity-seconds=86400",
    "auth.token.refresh-threshold-seconds=600",
    "auth.redis.refresh-token-key-prefix=test_refresh:",
    "auth.redis.user-tokens-key-prefix=test_user:",
    "auth.redis.email-verification-key-prefix=test_email:",
    "auth.redis.jwt-blacklist-key-prefix=test_jwt:",
    "auth.messages.invalid-credentials=테스트 인증 실패",
    "auth.messages.invalid-refresh-token=테스트 리프레시 토큰 오류",
    "auth.messages.expired-refresh-token=테스트 만료된 토큰",
    "auth.messages.user-not-found=테스트 사용자 없음",
    "auth.messages.invalid-token-info=테스트 잘못된 토큰",
    "auth.messages.account-locked=테스트 계정 잠금",
    "auth.messages.verification-code-expired=테스트 인증 코드 만료"
})
class AuthPropertiesTest {

    @Autowired
    private AuthProperties authProperties;

    @Test
    @DisplayName("보안 설정이 올바르게 로드되는지 확인")
    void shouldLoadSecurityProperties() {
        // given & when
        AuthProperties.Security security = authProperties.getSecurity();

        // then
        assertThat(security.getMinResponseTimeMs()).isEqualTo(300);
        assertThat(security.getDummyHash()).isEqualTo("test-dummy-hash");
        assertThat(security.getMaxLoginAttempts()).isEqualTo(3);
        assertThat(security.getAccountLockDurationMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("토큰 설정이 올바르게 로드되는지 확인")
    void shouldLoadTokenProperties() {
        // given & when
        AuthProperties.Token token = authProperties.getToken();

        // then
        assertThat(token.getAccessTokenValiditySeconds()).isEqualTo(1800);
        assertThat(token.getRefreshTokenValiditySeconds()).isEqualTo(86400);
        assertThat(token.getRefreshThresholdSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("Redis 설정이 올바르게 로드되는지 확인")
    void shouldLoadRedisProperties() {
        // given & when
        AuthProperties.Redis redis = authProperties.getRedis();

        // then
        assertThat(redis.getRefreshTokenKeyPrefix()).isEqualTo("test_refresh:");
        assertThat(redis.getUserTokensKeyPrefix()).isEqualTo("test_user:");
        assertThat(redis.getEmailVerificationKeyPrefix()).isEqualTo("test_email:");
        assertThat(redis.getJwtBlacklistKeyPrefix()).isEqualTo("test_jwt:");
    }

    @Test
    @DisplayName("메시지 설정이 올바르게 로드되는지 확인")
    void shouldLoadMessageProperties() {
        // given & when
        AuthProperties.Messages messages = authProperties.getMessages();

        // then
        assertThat(messages.getInvalidCredentials()).isEqualTo("테스트 인증 실패");
        assertThat(messages.getInvalidRefreshToken()).isEqualTo("테스트 리프레시 토큰 오류");
        assertThat(messages.getExpiredRefreshToken()).isEqualTo("테스트 만료된 토큰");
        assertThat(messages.getUserNotFound()).isEqualTo("테스트 사용자 없음");
        assertThat(messages.getInvalidTokenInfo()).isEqualTo("테스트 잘못된 토큰");
        assertThat(messages.getAccountLocked()).isEqualTo("테스트 계정 잠금");
        assertThat(messages.getVerificationCodeExpired()).isEqualTo("테스트 인증 코드 만료");
    }

    @Test
    @DisplayName("기본값 설정 확인")
    void shouldHaveDefaultValues() {
        // given - 새로운 AuthProperties 인스턴스 (기본값 사용)
        AuthProperties defaultProps = new AuthProperties();

        // when & then - 기본값 확인
        assertThat(defaultProps.getSecurity().getMinResponseTimeMs()).isEqualTo(200);
        assertThat(defaultProps.getSecurity().getDummyHash()).contains("dummy.hash");
        assertThat(defaultProps.getSecurity().getMaxLoginAttempts()).isEqualTo(5);
        assertThat(defaultProps.getSecurity().getAccountLockDurationMinutes()).isEqualTo(30);

        assertThat(defaultProps.getToken().getAccessTokenValiditySeconds()).isEqualTo(15 * 60); // 15분
        assertThat(defaultProps.getToken().getRefreshTokenValiditySeconds()).isEqualTo(7 * 24 * 60 * 60); // 7일
        assertThat(defaultProps.getToken().getRefreshThresholdSeconds()).isEqualTo(5 * 60); // 5분

        assertThat(defaultProps.getRedis().getRefreshTokenKeyPrefix()).isEqualTo("refresh_token:");
        assertThat(defaultProps.getRedis().getUserTokensKeyPrefix()).isEqualTo("user_tokens:");
        assertThat(defaultProps.getRedis().getEmailVerificationKeyPrefix()).isEqualTo("email_verification:");
        assertThat(defaultProps.getRedis().getJwtBlacklistKeyPrefix()).isEqualTo("jwt:blacklist:");

        assertThat(defaultProps.getMessages().getInvalidCredentials()).contains("이메일 또는 비밀번호");
        assertThat(defaultProps.getMessages().getInvalidRefreshToken()).contains("유효하지 않은");
        assertThat(defaultProps.getMessages().getExpiredRefreshToken()).contains("만료");
        assertThat(defaultProps.getMessages().getUserNotFound()).contains("사용자를 찾을 수 없습니다");
        assertThat(defaultProps.getMessages().getInvalidTokenInfo()).contains("토큰 정보");
        assertThat(defaultProps.getMessages().getAccountLocked()).contains("계정이 일시적으로 잠겼습니다");
        assertThat(defaultProps.getMessages().getVerificationCodeExpired()).contains("인증 코드");
    }

    @Test
    @DisplayName("내부 클래스들이 올바르게 초기화되는지 확인")
    void shouldInitializeInnerClasses() {
        // when & then
        assertThat(authProperties.getSecurity()).isNotNull();
        assertThat(authProperties.getToken()).isNotNull();
        assertThat(authProperties.getRedis()).isNotNull();
        assertThat(authProperties.getMessages()).isNotNull();
    }

    @Test
    @DisplayName("ConfigurationProperties 어노테이션 확인")
    void shouldHaveConfigurationPropertiesAnnotation() {
        // when
        Class<AuthProperties> clazz = AuthProperties.class;

        // then
        assertThat(clazz.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationProperties.class)).isTrue();
        
        org.springframework.boot.context.properties.ConfigurationProperties annotation = 
            clazz.getAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class);
        assertThat(annotation.prefix()).isEqualTo("auth");
    }

    @Test
    @DisplayName("토큰 유효성 시간 범위 확인")
    void shouldValidateTokenValidityRange() {
        // given & when
        AuthProperties.Token token = authProperties.getToken();

        // then
        assertThat(token.getAccessTokenValiditySeconds()).isPositive();
        assertThat(token.getRefreshTokenValiditySeconds()).isPositive();
        assertThat(token.getRefreshThresholdSeconds()).isPositive();
        
        // 리프레시 토큰은 액세스 토큰보다 유효기간이 길어야 함
        assertThat(token.getRefreshTokenValiditySeconds()).isGreaterThan(token.getAccessTokenValiditySeconds());
    }

    @Test
    @DisplayName("보안 설정 값 범위 확인")
    void shouldValidateSecurityConfigRange() {
        // given & when
        AuthProperties.Security security = authProperties.getSecurity();

        // then
        assertThat(security.getMinResponseTimeMs()).isPositive();
        assertThat(security.getMaxLoginAttempts()).isPositive();
        assertThat(security.getAccountLockDurationMinutes()).isPositive();
        assertThat(security.getDummyHash()).isNotBlank();
    }
}