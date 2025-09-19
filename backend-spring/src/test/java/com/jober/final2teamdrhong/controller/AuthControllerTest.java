package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.userLogin.TokenRefreshResponse;
import com.jober.final2teamdrhong.dto.userLogout.UserLogoutRequest;
import com.jober.final2teamdrhong.service.AuthService;
import com.jober.final2teamdrhong.service.EmailService;
import com.jober.final2teamdrhong.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private JwtConfig jwtConfig;

    @Test
    @DisplayName("성공: 토큰 갱신")
    void refreshToken_success() throws Exception {
        // given
        String authorizationHeader = "Bearer refresh-token";
        TokenRefreshResponse response = TokenRefreshResponse.of(
                "new-access-token",
                "new-refresh-token",
                3600L
        );

        given(authService.refreshTokens(eq(authorizationHeader), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        then(authService).should().refreshTokens(eq(authorizationHeader), anyString());
    }

    @Test
    @DisplayName("실패: Authorization 헤더 없이 토큰 갱신")
    void refreshToken_fail_noAuthorizationHeader() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공: 로그아웃")
    void logout_success() throws Exception {
        // given
        String authorizationHeader = "Bearer access-token";
        String refreshToken = "refresh-token";

        UserLogoutRequest logoutRequest = new UserLogoutRequest(refreshToken);

        given(jwtConfig.extractTokenFromHeader(authorizationHeader)).willReturn("access-token");

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        then(authService).should().logout(eq("access-token"), eq(refreshToken), anyString());
    }

    @Test
    @DisplayName("성공: Authorization 헤더 없이 로그아웃")
    void logout_success_noAuthorizationHeader() throws Exception {
        // given
        String refreshToken = "refresh-token";

        UserLogoutRequest logoutRequest = new UserLogoutRequest(refreshToken);

        given(jwtConfig.extractTokenFromHeader(null)).willReturn(null);

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        then(authService).should().logout(eq(null), eq(refreshToken), anyString());
    }

    @Test
    @DisplayName("실패: RefreshToken 없이 로그아웃")
    void logout_fail_noRefreshToken() throws Exception {
        // given
        UserLogoutRequest logoutRequest = new UserLogoutRequest(""); // 빈 문자열

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isBadRequest());
    }
}