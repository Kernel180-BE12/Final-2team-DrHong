package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private AuthProperties authProperties;

    @Mock
    private AuthProperties.Token tokenConfig;

    @Mock
    private AuthProperties.Redis redisConfig;

    @Mock
    private AuthProperties.Messages messagesConfig;

    private User testUser;
    private String testEmail = "test@example.com";
    private Integer testUserId = 1;
    private String testClientIp = "127.0.0.1";

    @BeforeEach
    void setUp() {
        // Redis 연산 모킹
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // AuthProperties 설정
        lenient().when(authProperties.getToken()).thenReturn(tokenConfig);
        lenient().when(authProperties.getRedis()).thenReturn(redisConfig);
        lenient().when(authProperties.getMessages()).thenReturn(messagesConfig);

        // Token 설정
        lenient().when(tokenConfig.getRefreshTokenValiditySeconds()).thenReturn(604800L); // 7일

        // Redis 키 접두사 설정
        lenient().when(redisConfig.getRefreshTokenKeyPrefix()).thenReturn("refresh_token:");
        lenient().when(redisConfig.getUserTokensKeyPrefix()).thenReturn("user_tokens:");

        // 메시지 설정
        lenient().when(messagesConfig.getInvalidRefreshToken()).thenReturn("유효하지 않은 리프레시 토큰입니다.");
        lenient().when(messagesConfig.getExpiredRefreshToken()).thenReturn("만료되었거나 유효하지 않은 리프레시 토큰입니다.");
        lenient().when(messagesConfig.getUserNotFound()).thenReturn("사용자를 찾을 수 없습니다.");
        lenient().when(messagesConfig.getInvalidTokenInfo()).thenReturn("토큰 정보가 유효하지 않습니다.");

        // 테스트 사용자 생성
        testUser = User.builder()
                .userId(testUserId)
                .userEmail(testEmail)
                .userName("테스트사용자")
                .build();
    }

    @Test
    @DisplayName("✅ Refresh Token 생성 성공")
    void createRefreshToken_Success() {
        // given
        String expectedToken = "generated.refresh.token";
        String tokenHash = "token-hash";

        when(jwtConfig.generateRefreshToken(testEmail, testUserId)).thenReturn(expectedToken);
        when(jwtConfig.generateTokenHash(expectedToken)).thenReturn(tokenHash);

        // when
        String actualToken = refreshTokenService.createRefreshToken(testUser, testClientIp);

        // then
        assertThat(actualToken).isEqualTo(expectedToken);
        
        // Redis 저장 검증
        verify(valueOperations).set(
                eq("refresh_token:" + tokenHash),
                eq(testUserId.toString()),
                eq(Duration.ofSeconds(604800L))
        );
        verify(setOperations).add("user_tokens:" + testUserId, tokenHash);
        verify(redisTemplate).expire("user_tokens:" + testUserId, Duration.ofSeconds(604800L));
    }

    @Test
    @DisplayName("✅ Refresh Token으로 새 토큰 쌍 발급 성공 - 인증된 사용자")
    void refreshTokens_Success() {
        // given
        String oldRefreshToken = "old.refresh.token";
        String oldTokenHash = "old-token-hash";
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";
        String newTokenHash = "new-token-hash";
        
        // 인증된 UserAuth 생성
        UserAuth verifiedAuth = UserAuth.builder()
                .authId(1)
                .authType(UserAuth.AuthType.LOCAL)
                .isVerified(true)  // 인증 완료
                .build();
        
        testUser.addUserAuth(verifiedAuth);

        when(jwtConfig.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtConfig.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtConfig.getEmailFromToken(oldRefreshToken)).thenReturn(testEmail);
        when(jwtConfig.getUserIdFromToken(oldRefreshToken)).thenReturn(testUserId.longValue());
        when(jwtConfig.generateTokenHash(oldRefreshToken)).thenReturn(oldTokenHash);
        when(redisTemplate.hasKey("refresh_token:" + oldTokenHash)).thenReturn(true);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtConfig.generateAccessToken(testEmail, testUserId)).thenReturn(newAccessToken);
        when(jwtConfig.generateRefreshToken(testEmail, testUserId)).thenReturn(newRefreshToken);
        when(jwtConfig.generateTokenHash(newRefreshToken)).thenReturn(newTokenHash);
        when(valueOperations.get("refresh_token:" + oldTokenHash)).thenReturn(testUserId.toString());

        // when
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.refreshTokens(oldRefreshToken, testClientIp);

        // then
        assertThat(tokenPair.accessToken()).isEqualTo(newAccessToken);
        assertThat(tokenPair.refreshToken()).isEqualTo(newRefreshToken);

        // 기존 토큰 무효화 검증
        verify(redisTemplate).delete("refresh_token:" + oldTokenHash);
        verify(setOperations).remove("user_tokens:" + testUserId, oldTokenHash);

        // 새 토큰 저장 검증
        verify(valueOperations).set(
                eq("refresh_token:" + newTokenHash),
                eq(testUserId.toString()),
                eq(Duration.ofSeconds(604800L))
        );
    }

    @Test
    @DisplayName("❌ 유효하지 않은 Refresh Token으로 갱신 실패")
    void refreshTokens_InvalidToken_ThrowsException() {
        // given
        String invalidToken = "invalid.refresh.token";
        when(jwtConfig.validateToken(invalidToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshTokens(invalidToken, testClientIp))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("❌ Redis에 없는 Refresh Token으로 갱신 실패")
    void refreshTokens_TokenNotInRedis_ThrowsException() {
        // given
        String refreshToken = "valid.refresh.token";
        String tokenHash = "token-hash";

        when(jwtConfig.validateToken(refreshToken)).thenReturn(true);
        when(jwtConfig.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtConfig.getEmailFromToken(refreshToken)).thenReturn(testEmail);
        when(jwtConfig.getUserIdFromToken(refreshToken)).thenReturn(testUserId.longValue());
        when(jwtConfig.generateTokenHash(refreshToken)).thenReturn(tokenHash);
        when(redisTemplate.hasKey("refresh_token:" + tokenHash)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshTokens(refreshToken, testClientIp))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("만료되었거나 유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("❌ 미인증 사용자로 토큰 갱신 실패")
    void refreshTokens_UnverifiedUser_ThrowsException() {
        // given
        String refreshToken = "valid.refresh.token";
        String tokenHash = "token-hash";
        
        // 미인증 UserAuth 생성
        UserAuth unverifiedAuth = UserAuth.builder()
                .authId(1)
                .authType(UserAuth.AuthType.LOCAL)
                .isVerified(false)  // 미인증 상태
                .build();
        
        testUser.addUserAuth(unverifiedAuth);

        when(jwtConfig.validateToken(refreshToken)).thenReturn(true);
        when(jwtConfig.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtConfig.getEmailFromToken(refreshToken)).thenReturn(testEmail);
        when(jwtConfig.getUserIdFromToken(refreshToken)).thenReturn(testUserId.longValue());
        when(jwtConfig.generateTokenHash(refreshToken)).thenReturn(tokenHash);
        when(redisTemplate.hasKey("refresh_token:" + tokenHash)).thenReturn(true);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshTokens(refreshToken, testClientIp))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("이메일 인증이 완료되지 않은 사용자입니다.");
    }

    @Test
    @DisplayName("❌ 존재하지 않는 사용자로 토큰 갱신 실패")
    void refreshTokens_UserNotFound_ThrowsException() {
        // given
        String refreshToken = "valid.refresh.token";
        String tokenHash = "token-hash";

        when(jwtConfig.validateToken(refreshToken)).thenReturn(true);
        when(jwtConfig.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtConfig.getEmailFromToken(refreshToken)).thenReturn(testEmail);
        when(jwtConfig.getUserIdFromToken(refreshToken)).thenReturn(testUserId.longValue());
        when(jwtConfig.generateTokenHash(refreshToken)).thenReturn(tokenHash);
        when(redisTemplate.hasKey("refresh_token:" + tokenHash)).thenReturn(true);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshTokens(refreshToken, testClientIp))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("✅ Refresh Token 무효화 성공")
    void revokeRefreshToken_Success() {
        // given
        String refreshToken = "refresh.token";
        String tokenHash = "token-hash";

        when(jwtConfig.generateTokenHash(refreshToken)).thenReturn(tokenHash);
        when(valueOperations.get("refresh_token:" + tokenHash)).thenReturn(testUserId.toString());

        // when
        refreshTokenService.revokeRefreshToken(refreshToken);

        // then
        verify(redisTemplate).delete("refresh_token:" + tokenHash);
        verify(setOperations).remove("user_tokens:" + testUserId, tokenHash);
    }

    @Test
    @DisplayName("✅ 사용자의 모든 Refresh Token 무효화 성공")
    void revokeAllUserTokens_Success() {
        // given
        Set<String> tokenHashes = Set.of("hash1", "hash2", "hash3");
        when(setOperations.members("user_tokens:" + testUserId)).thenReturn(tokenHashes);

        // when
        refreshTokenService.revokeAllUserTokens(testUserId.longValue());

        // then
        for (String hash : tokenHashes) {
            verify(redisTemplate).delete("refresh_token:" + hash);
        }
        verify(redisTemplate).delete("user_tokens:" + testUserId);
    }

    @Test
    @DisplayName("✅ 유효한 Refresh Token 검증 성공")
    void isValidRefreshToken_ValidToken_ReturnsTrue() {
        // given
        String refreshToken = "valid.refresh.token";
        String tokenHash = "token-hash";

        when(jwtConfig.validateToken(refreshToken)).thenReturn(true);
        when(jwtConfig.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtConfig.generateTokenHash(refreshToken)).thenReturn(tokenHash);
        when(redisTemplate.hasKey("refresh_token:" + tokenHash)).thenReturn(true);

        // when
        boolean isValid = refreshTokenService.isValidRefreshToken(refreshToken);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("❌ 유효하지 않은 Refresh Token 검증 실패")
    void isValidRefreshToken_InvalidToken_ReturnsFalse() {
        // given
        String invalidToken = "invalid.refresh.token";
        when(jwtConfig.validateToken(invalidToken)).thenReturn(false);

        // when
        boolean isValid = refreshTokenService.isValidRefreshToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("✅ Redis 예외 발생 시 처리")
    void handleRedisException_OnSave() {
        // given
        String expectedToken = "generated.refresh.token";
        String tokenHash = "token-hash";

        when(jwtConfig.generateRefreshToken(testEmail, testUserId)).thenReturn(expectedToken);
        when(jwtConfig.generateTokenHash(expectedToken)).thenReturn(tokenHash);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // when & then
        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(testUser, testClientIp))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to save Refresh Token");
    }
}