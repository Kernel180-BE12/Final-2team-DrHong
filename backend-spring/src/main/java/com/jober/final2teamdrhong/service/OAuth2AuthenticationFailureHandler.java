package com.jober.final2teamdrhong.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그인 실패 시 처리하는 핸들러
 * 에러 정보를 포함하여 프론트엔드 로그인 페이지로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {
        try {
            String errorMessage = determineErrorMessage(exception);

            log.error("OAuth2 로그인 실패 - 요청 URI: {}, 오류: {}",
                     request.getRequestURI(), exception.getMessage(), exception);

            // 프론트엔드 로그인 페이지로 에러 정보와 함께 리다이렉트
            String targetUrl = UriComponentsBuilder.fromUriString(getRedirectUrl())
                    .path("/login")
                    .queryParam("error", "oauth2_failure")
                    .queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 실패 핸들러에서 오류 발생: {}", e.getMessage(), e);

            // 최종 대안: 단순 에러 페이지로 리다이렉트
            String fallbackUrl = UriComponentsBuilder.fromUriString(getRedirectUrl())
                    .path("/login")
                    .queryParam("error", "system_error")
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, fallbackUrl);
        }
    }

    /**
     * 예외 유형에 따른 사용자 친화적 에러 메시지 생성
     */
    private String determineErrorMessage(AuthenticationException exception) {
        String exceptionMessage = exception.getMessage();
        String className = exception.getClass().getSimpleName();

        // 일반적인 OAuth2 오류 패턴 매칭
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("access_denied")) {
                return "사용자가 권한 승인을 취소했습니다.";
            } else if (exceptionMessage.contains("invalid_request")) {
                return "잘못된 요청입니다. 다시 시도해 주세요.";
            } else if (exceptionMessage.contains("unauthorized_client")) {
                return "인증되지 않은 클라이언트입니다.";
            } else if (exceptionMessage.contains("unsupported_response_type")) {
                return "지원하지 않는 응답 형식입니다.";
            } else if (exceptionMessage.contains("invalid_scope")) {
                return "유효하지 않은 권한 범위입니다.";
            } else if (exceptionMessage.contains("server_error")) {
                return "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            } else if (exceptionMessage.contains("temporarily_unavailable")) {
                return "서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";
            }
        }

        // 예외 클래스명에 따른 분류
        switch (className) {
            case "OAuth2AuthenticationException":
                return "소셜 로그인 인증에 실패했습니다.";
            case "OAuth2AuthorizationException":
                return "소셜 로그인 권한 부여에 실패했습니다.";
            case "HttpClientErrorException":
                return "네트워크 오류가 발생했습니다. 다시 시도해 주세요.";
            case "HttpServerErrorException":
                return "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            default:
                log.warn("처리되지 않은 OAuth2 예외 유형: {} - {}", className, exceptionMessage);
                return "소셜 로그인 중 오류가 발생했습니다. 다시 시도해 주세요.";
        }
    }

    /**
     * 리다이렉트할 프론트엔드 URL 반환
     */
    private String getRedirectUrl() {
        // allowedOrigins에서 첫 번째 URL 사용 (일반적으로 프론트엔드 URL)
        String[] origins = allowedOrigins.split(",");
        return origins[0].trim();
    }
}