package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.UserLogin.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserLogin.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignup.UserSignupRequest;
import com.jober.final2teamdrhong.service.UserService;
import com.jober.final2teamdrhong.service.EmailService;
import com.jober.final2teamdrhong.service.RateLimitService;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 단위 테스트
 * Mock을 사용하여 Controller 계층만 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RateLimitService rateLimitService;

    private UserSignupRequest validSignupRequest;
    private UserLoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validSignupRequest = UserSignupRequest.builder()
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
    @DisplayName("성공: 유효한 회원가입 요청")
    void signup_success() throws Exception {
        // given - Service Mock 설정
        // userService.signupWithRateLimit()은 void 메서드이므로 별도 설정 불필요

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."));

        // Service 메서드 호출 확인
        then(userService).should().signupWithRateLimit(any(UserSignupRequest.class), anyString());
    }

    @Test
    @DisplayName("실패: 잘못된 이메일 형식으로 회원가입")
    void signup_fail_invalid_email() throws Exception {
        // given
        UserSignupRequest invalidRequest = UserSignupRequest.builder()
                .userName("테스트유저")
                .email("invalid-email")  // 잘못된 이메일 형식
                .password("Password123!")
                .userNumber("010-1234-5678")
                .verificationCode("123456")
                .build();

        // when & then - ErrorResponse로 변경
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());  // ErrorResponse는 message 필드만 있음

        // Service 메서드 호출되지 않음 확인
        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공: 유효한 로그인 요청")
    void login_success() throws Exception {
        // given - Mock User 객체 생성
        User mockUser = User.create("테스트유저", "test@example.com", "010-1234-5678");
        UserLoginResponse mockResponse = UserLoginResponse.withRefreshToken(
                mockUser, "mock-access-token", "mock-refresh-token"
        );
        given(userService.loginWithRefreshToken(any(UserLoginRequest.class), anyString()))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));

        then(userService).should().loginWithRefreshToken(any(UserLoginRequest.class), anyString());
    }

    @Test
    @DisplayName("성공: 이메일 인증 코드 발송")
    void sendVerificationCode_success() throws Exception {
        // given
        String requestBody = "{\"email\":\"test@example.com\"}";

        // when & then
        mockMvc.perform(post("/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."));

        then(emailService).should().sendVerificationCodeWithRateLimit(any(String.class), anyString());
    }

    @Test
    @DisplayName("실패: 잘못된 JSON 형식")
    void request_fail_invalid_json() throws Exception {
        // given
        String invalidJson = "{invalid json}";

        // when & then - ErrorResponse 형식 검증
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청 형식입니다. JSON 형식을 확인해주세요."));

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패: Rate Limit 초과 - 회원가입")
    void signup_fail_rate_limit_exceeded() throws Exception {
        // given - Rate Limit 예외 Mock 설정
        willThrow(new RateLimitExceededException("회원가입 요청이 너무 많습니다. 3600초 후 다시 시도해주세요.", 3600L))
                .given(userService).signupWithRateLimit(any(UserSignupRequest.class), anyString());

        // when & then - ErrorResponse with retryAfterSeconds
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSignupRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"))
                .andExpect(jsonPath("$.message").value("회원가입 요청이 너무 많습니다. 3600초 후 다시 시도해주세요."))
                .andExpect(jsonPath("$.retryAfterSeconds").value(3600));

        then(userService).should().signupWithRateLimit(any(UserSignupRequest.class), anyString());
    }

    @Test
    @DisplayName("실패: Rate Limit 초과 - 이메일 발송")
    void sendVerificationCode_fail_rate_limit_exceeded() throws Exception {
        // given - Rate Limit 예외 Mock 설정
        willThrow(new RateLimitExceededException("이메일 발송 요청이 너무 많습니다. 300초 후 다시 시도해주세요.", 300L))
                .given(emailService).sendVerificationCodeWithRateLimit(anyString(), anyString());

        String requestBody = "{\"email\":\"test@example.com\"}";

        // when & then - ErrorResponse with retryAfterSeconds
        mockMvc.perform(post("/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(jsonPath("$.message").value("이메일 발송 요청이 너무 많습니다. 300초 후 다시 시도해주세요."))
                .andExpect(jsonPath("$.retryAfterSeconds").value(300));

        then(emailService).should().sendVerificationCodeWithRateLimit(anyString(), anyString());
    }
}