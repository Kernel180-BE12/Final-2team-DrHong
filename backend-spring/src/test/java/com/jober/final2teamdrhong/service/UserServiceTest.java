package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignupRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.exception.BusinessException;
import com.jober.final2teamdrhong.exception.DuplicateResourceException;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.mock;

/**
 * UserService 단위 테스트
 * Mock을 사용하여 의존성을 격리하고 순수한 비즈니스 로직만 테스트
 * H2 인메모리 DB 사용
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

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

    @InjectMocks
    private UserService userService;

    private UserSignupRequest validRequest;
    private UserLoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validRequest = UserSignupRequest.builder()
                .userName("테스트유저")
                .email("test@example.com")
                .password("Password123!")
                .userNumber("010-1234-5678")
                .verificationCode("123456")
                .build();

        validLoginRequest = UserLoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();
    }

    @Test
    @DisplayName("성공: 유효한 데이터로 회원가입")
    void signup_success() {
        // given
        given(userRepository.findByUserEmail(anyString())).willReturn(Optional.empty());
        given(verificationStorage.validateAndDelete(anyString(), anyString())).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willReturn(createMockUser());

        // when
        userService.signup(validRequest);

        // then
        then(userRepository).should().findByUserEmail("test@example.com");
        then(verificationStorage).should().validateAndDelete("test@example.com", "123456");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("실패: 중복된 이메일로 회원가입")
    void signup_fail_duplicateEmail() {
        // given
        given(userRepository.findByUserEmail(anyString())).willReturn(Optional.of(createMockUser()));

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("이미 가입된 이메일입니다.");

        then(verificationStorage).should(never()).find(anyString());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("실패: 인증 코드가 존재하지 않음")
    void signup_fail_verificationCodeNotFound() {
        // given
        given(userRepository.findByUserEmail(anyString())).willReturn(Optional.empty());
        given(verificationStorage.validateAndDelete(anyString(), anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("인증 코드가 일치하지 않거나 만료되었습니다.");

        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("실패: 인증 코드가 일치하지 않음")
    void signup_fail_verificationCodeMismatch() {
        // given
        given(userRepository.findByUserEmail(anyString())).willReturn(Optional.empty());
        given(verificationStorage.validateAndDelete(anyString(), anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.signup(validRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("인증 코드가 일치하지 않거나 만료되었습니다.");

        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("성공: Rate limiting과 함께 회원가입")
    void signupWithRateLimit_success() {
        // given
        String clientIp = "127.0.0.1";
        given(userRepository.findByUserEmail(anyString())).willReturn(Optional.empty());
        given(verificationStorage.validateAndDelete(anyString(), anyString())).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willReturn(createMockUser());

        // when
        userService.signupWithRateLimit(validRequest, clientIp);

        // then
        then(rateLimitService).should().checkSignupRateLimit(clientIp, "test@example.com");
        then(userRepository).should().save(any(User.class));
    }

    private User createMockUser() {
        return User.create(
                "테스트유저",
                "test@example.com",
                "010-1234-5678"
        );
    }

    private User createMockUserWithAuth() {
        User user = createMockUser();
        UserAuth localAuth = UserAuth.builder()
                .authType(UserAuth.AuthType.LOCAL)
                .passwordHash("encoded-password")
                .build();
        user.addUserAuth(localAuth);
        return user;
    }
}
