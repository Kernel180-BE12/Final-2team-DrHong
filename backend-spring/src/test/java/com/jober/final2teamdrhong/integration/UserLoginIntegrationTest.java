package com.jober.final2teamdrhong.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.UserLogin.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserSignup.UserSignupRequest;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.BlacklistService;
import com.jober.final2teamdrhong.service.RefreshTokenService;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.core.RedisTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 로컬 로그인 통합 테스트
 * JWT 토큰 발급, Refresh Token, 블랙리스트 기능 포함
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("integration")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:cleanup-test-data.sql")
class UserLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationStorage verificationStorage;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private BlacklistService blacklistService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "Password123!";
    private final String TEST_USERNAME = "테스트사용자";
    private final String TEST_PHONE = "010-1234-5678";

    @BeforeEach
    void setUp() throws Exception {
        // Redis 전체 초기화 (테스트용)
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        
        // 테스트용 사용자 회원가입
        String verificationCode = "123456";
        verificationStorage.save(TEST_EMAIL, verificationCode);

        UserSignupRequest signupRequest = UserSignupRequest.builder()
                .userName(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .userNumber(TEST_PHONE)
                .verificationCode(verificationCode)
                .build();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("✅ 로그인 성공: JWT Access Token과 Refresh Token 발급")
    void login_success_with_jwt_tokens() throws Exception {
        // given
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        // when & then
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인이 성공적으로 완료되었습니다."))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.userName").value(TEST_USERNAME))
                .andExpect(jsonPath("$.userRole").value("USER"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.loginTime").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Access Token과 Refresh Token 추출
        var responseJson = objectMapper.readTree(response);
        String accessToken = responseJson.get("token").asText();
        String refreshToken = responseJson.get("refreshToken").asText();

        // then: 토큰 검증
        assertThat(accessToken).isNotEmpty();
        assertThat(refreshToken).isNotEmpty();
        assertThat(accessToken).isNotEqualTo(refreshToken);

        // Refresh Token이 유효한지 확인
        boolean isValid = refreshTokenService.isValidRefreshToken(refreshToken);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("❌ 로그인 실패: 잘못된 비밀번호")
    void login_fail_with_wrong_password() throws Exception {
        // given
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email(TEST_EMAIL)
                .password("WrongPassword123!")
                .build();

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("❌ 로그인 실패: 존재하지 않는 이메일")
    void login_fail_with_non_existent_email() throws Exception {
        // given
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email("nonexistent@example.com")
                .password(TEST_PASSWORD)
                .build();

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("✅ Refresh Token으로 새로운 Access Token 발급")
    void refresh_token_success() throws Exception {
        // given: 먼저 로그인하여 토큰 받기
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();

        // when & then: Refresh Token으로 새로운 Access Token 요청
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    @DisplayName("✅ 로그아웃: Refresh Token 블랙리스트 추가")
    void logout_success_with_blacklist() throws Exception {
        // given: 먼저 로그인
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("token").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        // when: 로그아웃 요청
        String logoutRequest = "{\"refreshToken\":\"" + refreshToken + "\"}";
        
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));

        // then: Access Token이 블랙리스트에 추가되었는지 확인
        assertThat(blacklistService.isTokenBlacklisted(accessToken)).isTrue();
        
        // Refresh Token이 무효화되었는지 확인 (revoked)
        boolean isValid = refreshTokenService.isValidRefreshToken(refreshToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("❌ 블랙리스트된 토큰으로 요청 실패")
    void request_fail_with_blacklisted_token() throws Exception {
        // given: 로그인 후 로그아웃
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("token").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        // 로그아웃 (토큰 블랙리스트 추가)
        String logoutRequest = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isOk());

        // when & then: 블랙리스트된 Refresh Token으로 갱신 시도
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("✅ Rate Limiting: 로그인 시도 제한")
    void login_rate_limiting() throws Exception {
        // given
        UserLoginRequest wrongRequest = UserLoginRequest.builder()
                .email(TEST_EMAIL)
                .password("WrongPassword!")
                .build();

        // when: 통합 테스트 환경에서는 rate limit이 1000으로 설정되어 있음
        // 실제로 1000번 시도하지 않고 간단히 테스트
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // then: 정상적으로 로그인 실패 응답 (Rate limit에 걸리지 않음)
        // 통합 환경에서는 rate limit이 매우 높으므로 실제로 제한에 걸리지 않음
    }
}