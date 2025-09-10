package com.jober.final2teamdrhong.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("JWT 설정 검증 테스트")
class JwtConfigTest {

    @InjectMocks
    private JwtConfig jwtConfig;

    @Test
    @DisplayName("성공: 유효한 JWT 키로 검증 통과")
    void validateJwtSecret_validKey_success() {
        // given: 32자 이상의 유효한 키
        String validKey = "this-is-a-valid-jwt-secret-key-for-testing-more-than-32-chars";
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", validKey);
        
        // when & then: 예외 발생하지 않음
        assertThatCode(() -> jwtConfig.validateJwtSecret())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실패: JWT 키가 없으면 IllegalStateException 발생")
    void validateJwtSecret_emptyKey_throwsException() {
        // given: 빈 키
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", "");
        
        // when & then
        assertThatThrownBy(() -> jwtConfig.validateJwtSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret key must be configured");
    }
    
    @Test
    @DisplayName("실패: JWT 키가 null이면 IllegalStateException 발생")
    void validateJwtSecret_nullKey_throwsException() {
        // given: null 키
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", null);
        
        // when & then
        assertThatThrownBy(() -> jwtConfig.validateJwtSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret key must be configured");
    }

    @Test
    @DisplayName("실패: JWT 키가 32자 미만이면 IllegalStateException 발생")
    void validateJwtSecret_shortKey_throwsException() {
        // given: 32자 미만의 짧은 키
        String shortKey = "short-key";
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", shortKey);
        
        // when & then
        assertThatThrownBy(() -> jwtConfig.validateJwtSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret key must be at least 32 characters");
    }

    @Test
    @DisplayName("실패: 기본값 JWT 키 사용 시 IllegalStateException 발생")
    void validateJwtSecret_defaultKey_throwsException() {
        // given: 보안상 위험한 기본값
        String defaultKey = "your-super-super-long-and-secure-secret-key-for-jwt-hs256";
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", defaultKey);
        
        // when & then
        assertThatThrownBy(() -> jwtConfig.validateJwtSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Default JWT secret key detected. Please use a secure, unique key");
    }
    
    @Test
    @DisplayName("경고: 약한 패턴의 JWT 키 (반복 문자)")
    void validateJwtSecret_weakKeyWithRepeatedChars_logsWarning() {
        // given: 반복 문자가 있는 키 (4개 이상 연속)
        String weakKey = "this-is-a-weak-key-with-aaaa-repeated-chars-for-testing";
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", weakKey);
        
        // when & then: 경고는 로그로만 출력되므로 예외 발생하지 않음
        assertThatCode(() -> jwtConfig.validateJwtSecret())
                .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("경고: 약한 패턴의 JWT 키 (순차 문자)")
    void validateJwtSecret_weakKeyWithSequentialChars_logsWarning() {
        // given: 순차 문자가 있는 키
        String weakKey = "this-is-a-weak-key-with-abcd-sequential-chars-for-testing";
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", weakKey);
        
        // when & then: 경고는 로그로만 출력되므로 예외 발생하지 않음
        assertThatCode(() -> jwtConfig.validateJwtSecret())
                .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("성공: 강한 패턴의 JWT 키")
    void validateJwtSecret_strongKey_success() {
        // given: 무작위성이 높은 강한 키
        String strongKey = "Kj9$mN2#pQ8*rT5&vW7!xZ4@bC6%fG3^hJ1+lM0-nP9=sU8~yA7";
        ReflectionTestUtils.setField(jwtConfig, "jwtSecretKey", strongKey);
        
        // when & then
        assertThatCode(() -> jwtConfig.validateJwtSecret())
                .doesNotThrowAnyException();
    }
}