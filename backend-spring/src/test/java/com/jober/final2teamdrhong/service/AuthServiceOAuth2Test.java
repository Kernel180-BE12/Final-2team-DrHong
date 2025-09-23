package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.dto.auth.SocialSignupRequest;
import com.jober.final2teamdrhong.dto.auth.SocialSignupResponse;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.exception.BusinessException;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import com.jober.final2teamdrhong.util.TimingAttackProtection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

/**
 * AuthService OAuth2 관련 기능 테스트
 * 소셜 로그인 회원가입 완료 기능을 중점적으로 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceOAuth2Test {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2TempStorageService oAuth2TempStorageService;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RefreshTokenService refreshTokenService;

    // 누락된 Mock들 추가
    @Mock
    private VerificationStorage verificationStorage;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private AuthProperties authProperties;

    @Mock
    private TimingAttackProtection timingAttackProtection;

    @InjectMocks
    private AuthService authService;

    private OAuth2TempUserInfo validTempUserInfo;
    private SocialSignupRequest validSignupRequest;

    @BeforeEach
    void setUp() {
        validTempUserInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        validSignupRequest = new SocialSignupRequest(
                "oauth2_temp:test-key",
                "010-1234-5678",
                true,
                true,
                false
        );
    }

    @Test
    @DisplayName("소셜 회원가입 완료 성공")
    void completeSocialSignup_ValidRequest_Success() {
        // Given
        given(oAuth2TempStorageService.retrieveTempUserInfo(validSignupRequest.tempKey()))
                .willReturn(validTempUserInfo);

        // 완전히 Mock된 User 객체 사용
        User mockSavedUser = mock(User.class);
        given(mockSavedUser.getUserId()).willReturn(1);
        given(mockSavedUser.getUserEmail()).willReturn("test@example.com");
        given(mockSavedUser.getUserName()).willReturn("테스트 사용자");
        given(mockSavedUser.getUserRole()).willReturn(User.UserRole.USER);

        given(userRepository.save(any(User.class))).willReturn(mockSavedUser);
        given(jwtConfig.generateAccessToken("test@example.com", 1)).willReturn("access-token");
        given(refreshTokenService.createRefreshToken(mockSavedUser, "social-signup")).willReturn("refresh-token");

        // When
        SocialSignupResponse response = authService.completeSocialSignup(validSignupRequest);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.email()).isEqualTo(validTempUserInfo.email());
        assertThat(response.name()).isEqualTo(validTempUserInfo.name());
        assertThat(response.provider()).isEqualTo(validTempUserInfo.provider());
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        // UserValidationService 호출 확인
        then(userValidationService).should().validateSocialSignupBusinessRules(
                validTempUserInfo.email(),
                validTempUserInfo.name(),
                validSignupRequest
        );

        // 임시 정보 삭제 확인
        then(oAuth2TempStorageService).should().deleteTempUserInfo(validSignupRequest.tempKey());
    }

    @Test
    @DisplayName("존재하지 않는 임시 키로 회원가입 시도 시 예외 발생")
    void completeSocialSignup_InvalidTempKey_ThrowsException() {
        // Given
        given(oAuth2TempStorageService.retrieveTempUserInfo(validSignupRequest.tempKey()))
                .willReturn(null);

        // When & Then
        assertThatThrownBy(() -> authService.completeSocialSignup(validSignupRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("임시 사용자 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("유효하지 않은 임시 사용자 정보 처리")
    void completeSocialSignup_InvalidTempUserInfo_ThrowsException() {
        // Given - 이메일이 없는 무효한 임시 정보
        OAuth2TempUserInfo invalidTempInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                null, // 이메일 없음
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        given(oAuth2TempStorageService.retrieveTempUserInfo(validSignupRequest.tempKey()))
                .willReturn(invalidTempInfo);

        // When & Then
        assertThatThrownBy(() -> authService.completeSocialSignup(validSignupRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("임시 사용자 정보가 손상되었습니다");
    }

    private User createMockUser() {
        return User.createForSocialSignup(
                validTempUserInfo.name(),
                validTempUserInfo.email(),
                validSignupRequest.getNormalizedPhoneNumber()
        );
    }
}