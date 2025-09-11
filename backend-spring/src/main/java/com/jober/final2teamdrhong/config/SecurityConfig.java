package com.jober.final2teamdrhong.config;

import com.jober.final2teamdrhong.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    // JWT 필터 주입
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Value("${app.environment.development:true}")
    private boolean isDevelopment;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    // 클릭재킹 공격 방지 (X-Frame-Options)
                    headers.frameOptions(frameOptions -> frameOptions.deny());
                    // MIME 타입 스니핑 방지 (X-Content-Type-Options)
                    headers.contentTypeOptions(contentType -> {});

                    // HTTPS 강제 (개발환경에서는 비활성화)
                    if (!isDevelopment) {
                        headers.httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000) // 1년
                                .includeSubDomains(true)
                                .preload(true)
                        );
                    }

                    // 커스텀 보안 헤더 추가
                    headers.addHeaderWriter((request, response) -> {
                        // XSS 보호 헤더 (X-XSS-Protection) - 레거시 지원
                        response.setHeader("X-XSS-Protection", "1; mode=block");
                        
                        // 레퍼러 정책 설정 (Referrer-Policy)
                        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                        
                        // 권한 정책 설정 (Permissions-Policy)
                        response.setHeader("Permissions-Policy",
                                "geolocation=(), microphone=(), camera=(), fullscreen=(self)");
                        
                        // Content Security Policy (CSP) 헤더
                        String cspPolicy = isDevelopment 
                            ? buildDevelopmentCSP() 
                            : buildProductionCSP();
                        response.setHeader("Content-Security-Policy", cspPolicy);
                        
                        // 추가 보안 헤더들
                        // Cross-Origin-Embedder-Policy
                        response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                        
                        // Cross-Origin-Opener-Policy
                        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                        
                        // Cross-Origin-Resource-Policy
                        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
                        
                        // Expect-CT (Certificate Transparency)
                        if (!isDevelopment) {
                            response.setHeader("Expect-CT", "max-age=86400, enforce");
                        }
                        
                        // Server 헤더 숨기기 (정보 노출 방지)
                        response.setHeader("Server", "");
                    });
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/signup", "/auth/send-verification-code", "/auth/login", "/auth/refresh", "/auth/logout").permitAll() // 회원가입 및 로그인 관련 API는 누구나 접근 가능
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll() // Swagger UI는 누구나 접근 가능
                        .anyRequest().authenticated() // 나머지 API는 인증된 사용자만 접근 가능
                )
                // 여기에 JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경별 CORS 설정
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // 프로덕션 환경에서는 더 엄격한 보안 설정
        if (!isDevelopment) {
            // 프로덕션에서는 와일드카드 도메인 허용 금지
            origins.forEach(origin -> {
                if (origin.contains("*")) {
                    throw new IllegalArgumentException(
                            "프로덕션 환경에서는 와일드카드 도메인을 허용할 수 없습니다: " + origin
                    );
                }
            });
        }

        // 허용할 HTTP 메서드 명시적 지정
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 허용할 헤더 보안 강화 (필요한 헤더만 명시적 허용)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Cache-Control"
        ));

        // 쿠키 기반 인증 허용 (JWT와 함께 사용시 필요)
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 설정 (성능 최적화)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // No interceptors added for now, as RequestTimingInterceptor is removed
    }
    
    /**
     * 개발 환경용 CSP 정책 (덜 엄격함)
     */
    private String buildDevelopmentCSP() {
        return String.join("; ", 
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'", // 개발용 Swagger, HMR 등 허용
            "style-src 'self' 'unsafe-inline'", // 인라인 스타일 허용 (개발 편의)
            "img-src 'self' data: https:",
            "font-src 'self' data:",
            "connect-src 'self' ws: wss:", // WebSocket 허용 (개발 서버)
            "media-src 'self'",
            "object-src 'none'",
            "frame-src 'none'",
            "base-uri 'self'",
            "form-action 'self'"
        );
    }
    
    /**
     * 프로덕션 환경용 CSP 정책 (엄격함)
     */
    private String buildProductionCSP() {
        // 허용된 도메인들 추출
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        String allowedDomains = String.join(" ", origins);
        
        return String.join("; ", 
            "default-src 'self'",
            "script-src 'self' 'strict-dynamic'", // nonce 기반 스크립트 허용
            "style-src 'self' 'unsafe-inline'", // CSS는 해시 기반으로 제한
            "img-src 'self' data: https:",
            "font-src 'self'",
            "connect-src 'self' " + allowedDomains, // API 호출 허용 도메인
            "media-src 'self'",
            "object-src 'none'",
            "frame-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "upgrade-insecure-requests", // HTTP를 HTTPS로 업그레이드
            "block-all-mixed-content" // 혼합 콘텐츠 차단
        );
    }
}