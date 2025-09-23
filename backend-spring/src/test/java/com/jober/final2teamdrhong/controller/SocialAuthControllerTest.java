package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.dto.auth.SocialSignupRequest;
import com.jober.final2teamdrhong.dto.auth.SocialSignupResponse;
import com.jober.final2teamdrhong.service.AuthService;
import com.jober.final2teamdrhong.service.OAuth2TempStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 소셜 인증 컨트롤러 테스트
 * OAuth2 관련 API 엔드포인트를 검증합니다.
 */
@WebMvcTest(SocialAuthController.class)
class SocialAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private OAuth2TempStorageService oAuth2TempStorageService;

    @Test
    @DisplayName("구글 소셜 로그인 시작 - 리다이렉트 성공")
    void loginWithGoogle_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/auth/social/login/google"))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api/oauth2/authorization/google"));
    }

    @Test
    @DisplayName("지원하는 소셜 로그인 제공자 목록 조회 성공")
    void getSupportedProviders_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/auth/social/providers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.google.name").value("Google"))
                .andExpect(jsonPath("$.providers.google.loginUrl").value("/api/auth/social/login/google"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("OAuth2 임시 사용자 정보 조회 성공")
    void getTempUserInfo_ValidKey_Success() throws Exception {
        // Given
        String tempKey = "oauth2_temp:test-key";
        OAuth2TempUserInfo tempUserInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        given(oAuth2TempStorageService.retrieveTempUserInfo(tempKey))
                .willReturn(tempUserInfo);
        given(oAuth2TempStorageService.getTempUserInfoTTL(tempKey))
                .willReturn(600L);

        // When & Then
        mockMvc.perform(get("/auth/social/temp-info")
                        .param("tempKey", tempKey))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트 사용자"))
                .andExpect(jsonPath("$.provider").value("google"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600));
    }

    @Test
    @DisplayName("존재하지 않는 임시 키로 조회 시 404 반환")
    void getTempUserInfo_NonExistentKey_ReturnsNotFound() throws Exception {
        // Given
        String tempKey = "oauth2_temp:non-existent";
        given(oAuth2TempStorageService.retrieveTempUserInfo(tempKey))
                .willReturn(null);

        // When & Then
        mockMvc.perform(get("/auth/social/temp-info")
                        .param("tempKey", tempKey))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("소셜 회원가입 완료 성공")
    @WithMockUser
    void completeSocialSignup_ValidRequest_Success() throws Exception {
        // Given
        SocialSignupRequest request = new SocialSignupRequest(
                "oauth2_temp:test-key",
                "010-1234-5678",
                true,
                true,
                false
        );

        SocialSignupResponse response = new SocialSignupResponse(
                true,
                "소셜 회원가입이 성공적으로 완료되었습니다.",
                1,
                "access-token",
                "refresh-token",
                "test@example.com",
                "테스트 사용자",
                "USER",
                "google"
        );

        given(authService.completeSocialSignup(any(SocialSignupRequest.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/social/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.provider").value("google"));
    }

    @Test
    @DisplayName("유효하지 않은 회원가입 요청 시 400 반환")
    @WithMockUser
    void completeSocialSignup_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given - 필수 필드가 누락된 요청
        SocialSignupRequest invalidRequest = new SocialSignupRequest(
                "", // 빈 tempKey
                "010-1234-5678",
                true,
                true,
                false
        );

        // When & Then
        mockMvc.perform(post("/auth/social/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("임시 정보 만료 시간 연장 성공")
    void extendTempInfo_ValidKey_Success() throws Exception {
        // Given
        String tempKey = "oauth2_temp:test-key";
        given(oAuth2TempStorageService.extendTempUserInfo(tempKey, 10))
                .willReturn(true);
        given(oAuth2TempStorageService.getTempUserInfoTTL(tempKey))
                .willReturn(1200L);

        // When & Then
        mockMvc.perform(post("/auth/social/extend-temp-info")
                        .param("tempKey", tempKey)
                        .param("additionalMinutes", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("만료 시간이 연장되었습니다."))
                .andExpect(jsonPath("$.extendedMinutes").value(10))
                .andExpect(jsonPath("$.newExpiresInSeconds").value(1200));
    }

    @Test
    @DisplayName("존재하지 않는 키로 만료 시간 연장 시 404 반환")
    void extendTempInfo_NonExistentKey_ReturnsNotFound() throws Exception {
        // Given
        String tempKey = "oauth2_temp:non-existent";
        given(oAuth2TempStorageService.extendTempUserInfo(tempKey, 10))
                .willReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/social/extend-temp-info")
                        .param("tempKey", tempKey)
                        .param("additionalMinutes", "10"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}