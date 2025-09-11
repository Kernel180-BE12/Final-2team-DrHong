package com.jober.final2teamdrhong.filter;

import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.JwtClaims;
import com.jober.final2teamdrhong.service.JwtClaimsService;
import com.jober.final2teamdrhong.service.BlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try{
            // 1. Authorization 헤더에서 토큰 추출
            String token = jwtConfig.extractTokenFromHeader(request.getHeader("Authorization"));

            // 2. 토큰이 유효한지 검증 (서명, 만료시간, 블랙리스트)
            if (token != null && jwtConfig.validateToken(token) && !blacklistService.isTokenBlacklisted(token)) {

                // 3. 토큰에서 기본 Claims를 추출하고 DB정보로 보완
                JwtClaims claims = jwtClaimsService.getEnrichedClaims(token);

                // 4. 인증 객체 (Authentication) 생성
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        claims, // Principal (사용자 정보 객체)
                        null,   // Credentials (JWT에서는 비밀번호 사용 안 함)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+claims.getUserRole().name()))// 권한 정보
                );

                // 5. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("사용자 인증 완료: {}", claims.getEmail());
            }
        }catch (Exception e){
            // 인증 관련 예외 발생 시 로그 기록
            log.warn("인증 처리 실패: {}",e.getMessage());
            //필터 체인에서 예외가 발생하면 Spring Security가 처리하도록 함 (401 Unauthorized 등 )
        }

        // 다음필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
