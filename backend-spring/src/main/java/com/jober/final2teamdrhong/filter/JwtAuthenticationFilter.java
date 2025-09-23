package com.jober.final2teamdrhong.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.exception.ErrorResponse;
import com.jober.final2teamdrhong.service.JwtClaimsService;
import com.jober.final2teamdrhong.service.BlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final JwtClaimsService jwtClaimsService;
    private final BlacklistService blacklistService;
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 인증이 필요한 엔드포인트인지 확인
        if (isPublicEndpoint(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 이미 인증이 설정되어 있는 경우 (테스트 환경에서 Mock 인증 등) 필터를 건너뜀
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        try{
            // 1. Authorization 헤더에서 토큰 추출
            String token = jwtConfig.extractTokenFromHeader(request.getHeader("Authorization"));

            // 2. 토큰 없는 경우 401 에러 처리
            if (token == null) {
                handleAuthenticationError(request, response, "인증 토큰이 제공되지 않았습니다.");
                return;
            }

            // 3. 토큰이 블랙리스트에 있는 경우 401 에러 처리  
            if (blacklistService.isTokenBlacklisted(token)) {
                handleAuthenticationError(request, response, "무효화된 토큰입니다. 다시 로그인해주세요.");
                return;
            }

            // 4. 토큰 유효성 검증
            if (!jwtConfig.validateToken(token)) {
                String errorMessage = determineJwtErrorMessage(request, new Exception("Token validation failed"));
                handleAuthenticationError(request, response, errorMessage);
                return;
            }

            // 5. 토큰에서 기본 Claims를 추출하고 DB정보로 보완
            JwtClaims claims = jwtClaimsService.getEnrichedClaims(token);

            // 6. 인증 객체 (Authentication) 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    claims, // Principal (사용자 정보 객체)
                    null,   // Credentials (JWT에서는 비밀번호 사용 안 함)
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+claims.getUserRole().name()))// 권한 정보
            );

            // 7. SecurityContext에 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("사용자 인증 완료: {}", claims.getEmail());
            
        }catch (Exception e){
            // JWT 관련 예외 발생 시 상세한 에러 메시지로 401 응답
            String errorMessage = determineJwtErrorMessage(request, e);
            handleAuthenticationError(request, response, errorMessage);
            return;
        }

        // 다음필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * JWT 인증 실패 시 401 에러 응답 처리 헬퍼 메서드
     */
    private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response, String errorMessage) 
            throws IOException {
        log.warn("JWT 인증 실패 - 401 Unauthorized: URI={}, 원인={}", 
                request.getRequestURI(), errorMessage);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorMessage);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 권한 부족 시 403 에러 응답 처리 헬퍼 메서드
     */
    private void handleAccessDeniedError(HttpServletRequest request, HttpServletResponse response, String errorMessage) 
            throws IOException {
        log.warn("권한 부족 - 403 Forbidden: URI={}, 원인={}", 
                request.getRequestURI(), errorMessage);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorMessage);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * JWT 토큰 에러 메시지 판단 헬퍼 메서드
     */
    private String determineJwtErrorMessage(HttpServletRequest request, Exception e) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.isEmpty()) {
            return "인증 토큰이 제공되지 않았습니다.";
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return "잘못된 인증 토큰 형식입니다. 'Bearer {token}' 형식으로 제공해주세요.";
        }
        
        // JWT 관련 예외 메시지 분석
        String errorMsg = e.getMessage();
        if (errorMsg.contains("expired") || errorMsg.contains("만료")) {
            return "토큰이 만료되었습니다. 새로운 토큰으로 다시 시도해주세요.";
        }
        
        if (errorMsg.contains("signature") || errorMsg.contains("서명")) {
            return "유효하지 않은 토큰입니다.";
        }
        
        if (errorMsg.contains("malformed") || errorMsg.contains("형식")) {
            return "잘못된 토큰 형식입니다.";
        }
        
        return "인증에 실패했습니다. 유효한 토큰을 제공해주세요.";
    }

    /**
     * 공개 엔드포인트 여부 확인 헬퍼 메서드
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/api/swagger-ui") ||
               requestURI.startsWith("/api/v3/api-docs") ||
               requestURI.startsWith("/api/swagger-resources") ||
               requestURI.startsWith("/api/webjars");
    }
}
