package com.jober.final2teamdrhong.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.auth.OAuth2UserInfo;
import com.jober.final2teamdrhong.dto.auth.OAuth2UserInfoFactory;
import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 처리하는 핸들러
 * 기존 회원인 경우 JWT 토큰을 발급하고,
 * 신규 회원인 경우 임시 정보를 저장하고 핸드폰 번호 입력 페이지로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2TempStorageService oAuth2TempStorageService;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // OAuth2User에서 추가 정보 추출
            Map<String, Object> attributes = oauth2User.getAttributes();
            Boolean isExistingUser = (Boolean) attributes.get("isExistingUser");
            String provider = (String) attributes.get("provider");

            log.info("OAuth2 로그인 성공 - 제공자: {}, 기존 회원: {}", provider, isExistingUser);

            if (Boolean.TRUE.equals(isExistingUser)) {
                // 기존 회원 처리 - JWT 토큰 발급
                handleExistingUser(request, response, oauth2User);
            } else {
                // 신규 회원 처리 - 임시 정보 저장 후 핸드폰 번호 입력 페이지로 리다이렉트
                handleNewUser(request, response, oauth2User, provider);
            }

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생: {}", e.getMessage(), e);
            handleError(request, response, "OAuth2 로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 기존 회원 처리 - JWT 토큰 발급 후 메인 페이지로 리다이렉트
     */
    private void handleExistingUser(HttpServletRequest request, HttpServletResponse response, OAuth2User oauth2User) throws IOException {
        try {
            // 사용자 ID 추출
            Integer userId = (Integer) oauth2User.getAttributes().get("userId");
            String userRole = (String) oauth2User.getAttributes().get("userRole");

            if (userId == null || userRole == null) {
                throw new IllegalStateException("사용자 정보가 불완전합니다.");
            }

            // 사용자 정보 조회 (이메일을 위해)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            // JWT 토큰 발급
            String accessToken = jwtConfig.generateAccessToken(user.getUserEmail(), userId);
            String refreshToken = refreshTokenService.createRefreshToken(user, getClientIp(request));

            log.info("기존 사용자 JWT 토큰 발급 완료 - 사용자 ID: {}", userId);

            // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
            String targetUrl = UriComponentsBuilder.fromUriString(getRedirectUrl())
                    .queryParam("success", "true")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("isNewUser", "false")
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("기존 회원 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("기존 회원 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 신규 회원 처리 - 임시 정보 저장 후 핸드폰 번호 입력 페이지로 리다이렉트
     */
    private void handleNewUser(HttpServletRequest request, HttpServletResponse response, OAuth2User oauth2User, String provider) throws IOException {
        try {
            // OAuth2 사용자 정보 파싱
            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                    provider,
                    oauth2User.getAttributes()
            );

            // 임시 사용자 정보 생성
            OAuth2TempUserInfo tempUserInfo = OAuth2TempUserInfo.fromOAuth2UserInfo(oAuth2UserInfo, provider);

            // OAuth2TempStorageService를 통해 임시 정보 저장
            String tempKey = oAuth2TempStorageService.storeTempUserInfo(tempUserInfo);

            log.info("신규 사용자 임시 정보 저장 완료 - 키: {}, 이메일: {}", tempKey, oAuth2UserInfo.getEmail());

            // 프론트엔드 핸드폰 번호 입력 페이지로 리다이렉트
            String targetUrl = UriComponentsBuilder.fromUriString(getRedirectUrl())
                    .path("/signup/phone")
                    .queryParam("tempKey", tempKey)
                    .queryParam("isNewUser", "true")
                    .queryParam("provider", provider)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("신규 회원 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("신규 회원 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 오류 처리 - 에러 페이지로 리다이렉트
     */
    private void handleError(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(getRedirectUrl())
                .path("/login")
                .queryParam("error", "oauth2_error")
                .queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 리다이렉트할 프론트엔드 URL 반환
     */
    private String getRedirectUrl() {
        // allowedOrigins에서 첫 번째 URL 사용 (일반적으로 프론트엔드 URL)
        String[] origins = allowedOrigins.split(",");
        return origins[0].trim();
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}