package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignupRequest;
import com.jober.final2teamdrhong.service.UserService;
import com.jober.final2teamdrhong.service.EmailService;
import com.jober.final2teamdrhong.entity.User;
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

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

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

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }
}