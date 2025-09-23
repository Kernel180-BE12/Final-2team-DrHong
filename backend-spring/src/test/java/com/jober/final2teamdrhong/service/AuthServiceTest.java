package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.userLogin.TokenRefreshResponse;
import com.jober.final2teamdrhong.dto.userLogin.UserLoginRequest;
import com.jober.final2teamdrhong.dto.userLogin.UserLoginResponse;
import com.jober.final2teamdrhong.dto.userSignup.UserSignupRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.exception.RateLimitExceededException;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import com.jober.final2teamdrhong.util.TimingAttackProtection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private VerificationStorage verificationStorage;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthProperties authProperties;

    @Mock
    private TimingAttackProtection timingAttackProtection;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private OAuth2TempStorageService oAuth2TempStorageService;

    @Mock
    private UserValidationService userValidationService;

    @InjectMocks
    private AuthService authService;

    private UserSignupRequest validSignupRequest;
    private UserLoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validSignupRequest = new UserSignupRequest(
                "테스트유저",
                "test@example.com",
                "010-1234-5678",
                "Password123!",
                "123456"
        );

        validLoginRequest = new UserLoginRequest(
                "test@example.com",
                "Password123!"
        );
    }

    @Test
    @DisplayName("성공: 유효한 데이터로 회원가입")
    void signup_success() {
        // given
        given(verificationStorage.validateAndDelete(anyString(), anyString())).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willReturn(createMockUser());

        // when
        authService.signup(validSignupRequest);

        // then
        then(userValidationService).should().validateLocalSignupBusinessRules(validSignupRequest);
        then(rateLimitService).should().checkEmailVerifyRateLimit("test@example.com");
        then(verificationStorage).should().validateAndDelete("test@example.com", "123456");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("실패: 중복된 이메일로 회원가입")
    void signup_fail_duplicateEmail() {
        // given
        willThrow(new com.jober.final2teamdrhong.exception.DuplicateResourceException("이미 가입된 이메일입니다."))
                .given(userValidationService).validateLocalSignupBusinessRules(validSignupRequest);

        // when & then
        assertThatThrownBy(() -> authService.signup(validSignupRequest))
                .isInstanceOf(com.jober.final2teamdrhong.exception.DuplicateResourceException.class)
                .hasMessageContaining("이미 가입된 이메일입니다.");

        then(verificationStorage).should(never()).validateAndDelete(anyString(), anyString());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("실패: 인증 코드가 일치하지 않으면 예외 발생")
    void signup_fail_invalidVerificationCode() {
        // given
        UserSignupRequest requestDto = new UserSignupRequest(
                "홍길동",
                "test@example.com",
                "010-1234-5678",
                "Test123!",
                "999999"
        );

        given(verificationStorage.validateAndDelete("test@example.com", "999999")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signup(requestDto))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("인증 코드가 일치하지 않거나 만료되었습니다.");

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("실패: 인증 코드가 만료되었으면 예외 발생")
    void signup_fail_expiredVerificationCode() {
        // given
        UserSignupRequest requestDto = new UserSignupRequest(
                "홍길동",
                "test@example.com",
                "010-1234-5678",
                "Test123!",
                "123456"
        );

        given(verificationStorage.validateAndDelete("test@example.com", "123456")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signup(requestDto))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("인증 코드가 일치하지 않거나 만료되었습니다.");

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("성공: Rate limiting 통과 시 회원가입")
    void signupWithRateLimit_success() {
        // given
        UserSignupRequest requestDto = new UserSignupRequest(
                "홍길동",
                "test@example.com",
                "010-1234-5678",
                "Test123!",
                "123456"
        );
        String clientIp = "192.168.1.1";

        given(verificationStorage.validateAndDelete("test@example.com", "123456")).willReturn(true);
        given(passwordEncoder.encode("Test123!")).willReturn("encoded_password");
        given(userRepository.save(any(User.class))).willReturn(createMockUser());

        // when
        authService.signupWithRateLimit(requestDto, clientIp);

        // then
        then(rateLimitService).should().checkSignupRateLimit(clientIp, "test@example.com");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("실패: Rate limiting 초과 시 예외 발생")
    void signupWithRateLimit_fail_rateLimitExceeded() {
        // given
        UserSignupRequest requestDto = new UserSignupRequest(
                "홍길동",
                "test@example.com",
                "010-1234-5678",
                "Test123!",
                "123456"
        );
        String clientIp = "192.168.1.1";

        willThrow(new RateLimitExceededException("회원가입 속도 제한을 초과했습니다. 3600초 후 다시 시도해주세요.", 3600L))
                .given(rateLimitService).checkSignupRateLimit(clientIp, "test@example.com");

        // when & then
        assertThatThrownBy(() -> authService.signupWithRateLimit(requestDto, clientIp))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("회원가입 속도 제한을 초과했습니다. 3600초 후 다시 시도해주세요.");

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("성공: 유효한 로그인 정보로 로그인")
    void loginWithRefreshToken_success() {
        // given
        String clientIp = "192.168.1.1";
        User mockUser = createMockUserWithAuth();

        // AuthProperties Mock 설정 추가
        AuthProperties.Security securityProps = mock(AuthProperties.Security.class);
        given(authProperties.getSecurity()).willReturn(securityProps);
        given(securityProps.getDummyHash()).willReturn("dummy-hash");

        given(userRepository.findByUserEmailWithAuth("test@example.com")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("Password123!", "encoded-password")).willReturn(true);
        given(jwtConfig.generateAccessToken("test@example.com", mockUser.getUserId())).willReturn("access-token");
        given(refreshTokenService.createRefreshToken(mockUser, clientIp)).willReturn("refresh-token");
        given(userRepository.save(mockUser)).willReturn(mockUser);

        // when
        UserLoginResponse response = authService.loginWithRefreshToken(validLoginRequest, clientIp);

        // then
        then(userRepository).should().findByUserEmailWithAuth("test@example.com");
        then(passwordEncoder).should().matches("Password123!", "encoded-password");
        then(jwtConfig).should().generateAccessToken("test@example.com", mockUser.getUserId());
        then(refreshTokenService).should().createRefreshToken(mockUser, clientIp);
        then(userRepository).should().save(mockUser);
        then(timingAttackProtection).should().startTiming();
        then(timingAttackProtection).should().clear();

        // response 검증
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("실패: 잘못된 비밀번호로 로그인")
    void loginWithRefreshToken_fail_wrongPassword() {
        // given
        String clientIp = "192.168.1.1";
        User mockUser = createMockUserWithAuth();

        given(userRepository.findByUserEmailWithAuth("test@example.com")).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches("Password123!", "encoded-password")).willReturn(false);

        AuthProperties.Security securityProps = mock(AuthProperties.Security.class);
        AuthProperties.Messages messagesProps = mock(AuthProperties.Messages.class);
        given(authProperties.getSecurity()).willReturn(securityProps);
        given(authProperties.getMessages()).willReturn(messagesProps);
        given(securityProps.getDummyHash()).willReturn("dummy-hash");
        given(securityProps.getMinResponseTimeMs()).willReturn(500);
        given(messagesProps.getInvalidCredentials()).willReturn("Invalid credentials");

        // when & then
        assertThatThrownBy(() -> authService.loginWithRefreshToken(validLoginRequest, clientIp))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        then(timingAttackProtection).should().startTiming();
        then(timingAttackProtection).should(atLeastOnce()).ensureMinimumResponseTime(500L);
        then(timingAttackProtection).should().clear();
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자로 로그인")
    void loginWithRefreshToken_fail_userNotFound() {
        // given
        String clientIp = "192.168.1.1";

        given(userRepository.findByUserEmailWithAuth("test@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.matches("Password123!", "dummy-hash")).willReturn(false);

        AuthProperties.Security securityProps = mock(AuthProperties.Security.class);
        AuthProperties.Messages messagesProps = mock(AuthProperties.Messages.class);
        given(authProperties.getSecurity()).willReturn(securityProps);
        given(authProperties.getMessages()).willReturn(messagesProps);
        given(securityProps.getDummyHash()).willReturn("dummy-hash");
        given(securityProps.getMinResponseTimeMs()).willReturn(500);
        given(messagesProps.getInvalidCredentials()).willReturn("Invalid credentials");

        // when & then
        assertThatThrownBy(() -> authService.loginWithRefreshToken(validLoginRequest, clientIp))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        then(timingAttackProtection).should().startTiming();
        then(timingAttackProtection).should(atLeastOnce()).ensureMinimumResponseTime(500L);
        then(timingAttackProtection).should().clear();
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("성공: 토큰 갱신")
    void refreshTokens_success() {
        // given
        String authorizationHeader = "Bearer refresh-token";
        String clientIp = "192.168.1.1";
        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        given(jwtConfig.extractTokenFromHeader(authorizationHeader)).willReturn(refreshToken);

        RefreshTokenService.TokenPair tokenPair = new RefreshTokenService.TokenPair(newAccessToken, newRefreshToken);
        given(refreshTokenService.refreshTokens(refreshToken, clientIp)).willReturn(tokenPair);
        given(jwtConfig.getAccessTokenValiditySeconds()).willReturn(3600L);

        // when
        TokenRefreshResponse response = authService.refreshTokens(authorizationHeader, clientIp);

        // then
        then(jwtConfig).should().extractTokenFromHeader(authorizationHeader);
        then(refreshTokenService).should().refreshTokens(refreshToken, clientIp);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(newAccessToken);
        assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("실패: Authorization 헤더 없이 토큰 갱신")
    void refreshTokens_fail_noAuthorizationHeader() {
        // given
        String authorizationHeader = "Bearer ";
        String clientIp = "192.168.1.1";

        given(jwtConfig.extractTokenFromHeader(authorizationHeader)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.refreshTokens(authorizationHeader, clientIp))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authorization 헤더가 필요합니다.");

        then(refreshTokenService).should(never()).refreshTokens(anyString(), anyString());
    }

    @Test
    @DisplayName("성공: 로그아웃 - Access Token과 Refresh Token 모두 유효")
    void logout_success_bothTokensValid() {
        // given
        String accessToken = "valid-access-token";
        String refreshToken = "valid-refresh-token";
        String clientIp = "192.168.1.1";

        given(jwtConfig.validateToken(accessToken)).willReturn(true);
        given(jwtConfig.isAccessToken(accessToken)).willReturn(true);
        given(jwtConfig.validateToken(refreshToken)).willReturn(true);
        given(jwtConfig.isRefreshToken(refreshToken)).willReturn(true);

        // when
        authService.logout(accessToken, refreshToken, clientIp);

        // then
        then(blacklistService).should().addAccessTokenToBlacklist(accessToken);
        then(refreshTokenService).should().revokeRefreshToken(refreshToken);
    }

    @Test
    @DisplayName("성공: 로그아웃 - Access Token만 유효")
    void logout_success_onlyAccessTokenValid() {
        // given
        String accessToken = "valid-access-token";
        String refreshToken = "invalid-refresh-token";
        String clientIp = "192.168.1.1";

        given(jwtConfig.validateToken(accessToken)).willReturn(true);
        given(jwtConfig.isAccessToken(accessToken)).willReturn(true);
        given(jwtConfig.validateToken(refreshToken)).willReturn(false);

        // when
        authService.logout(accessToken, refreshToken, clientIp);

        // then
        then(blacklistService).should().addAccessTokenToBlacklist(accessToken);
        then(refreshTokenService).should(never()).revokeRefreshToken(anyString());
    }

    @Test
    @DisplayName("성공: 로그아웃 - 토큰이 null인 경우")
    void logout_success_nullTokens() {
        // given
        String accessToken = null;
        String refreshToken = null;
        String clientIp = "192.168.1.1";

        // when
        authService.logout(accessToken, refreshToken, clientIp);

        // then
        then(blacklistService).should(never()).addAccessTokenToBlacklist(anyString());
        then(refreshTokenService).should(never()).revokeRefreshToken(anyString());
    }

    private User createMockUser() {
        return User.createForLocalSignup("테스트유저", "test@example.com", "010-1234-5678");
    }

    private User createMockUserWithAuth() {
        // Mockito로 User 객체를 완전히 mock 처리 (lenient로 불필요한 stubbing 허용)
        User user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(1);
        lenient().when(user.getUserName()).thenReturn("테스트유저");
        lenient().when(user.getUserEmail()).thenReturn("test@example.com");
        lenient().when(user.getUserNumber()).thenReturn("010-1234-5678");
        lenient().when(user.getUserRole()).thenReturn(User.UserRole.USER);

        // UserAuth도 mock 처리
        UserAuth userAuth = mock(UserAuth.class);
        lenient().when(userAuth.getAuthType()).thenReturn(UserAuth.AuthType.LOCAL);
        lenient().when(userAuth.getPasswordHash()).thenReturn("encoded-password");

        // UserAuth 리스트 mock 설정
        lenient().when(user.getUserAuths()).thenReturn(java.util.Collections.singletonList(userAuth));

        return user;
    }
}