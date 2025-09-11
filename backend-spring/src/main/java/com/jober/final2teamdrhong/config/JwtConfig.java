package com.jober.final2teamdrhong.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.jober.final2teamdrhong.dto.JwtClaims.JwtClaims;

import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Configuration
@Slf4j
public class JwtConfig {
    
    @Value("${jwt.secret.key:}")
    private String jwtSecretKey;
    
    @PostConstruct
    public void validateJwtSecret() {
        log.info("JWT 설정 검증 시작");
        
        // 1. JWT 키 존재 여부 확인
        if (!StringUtils.hasText(jwtSecretKey)) {
            log.error("JWT 시크릿 키가 설정되지 않았습니다. JWT_SECRET_KEY 환경변수를 설정해주세요.");
            throw new IllegalStateException("JWT secret key must be configured");
        }
        
        // 2. 최소 길이 검증 (HMAC-SHA256 권장 최소 길이)
        if (jwtSecretKey.length() < 32) {
            log.error("JWT 시크릿 키가 너무 짧습니다: {} 자 (최소 32자 필요)", jwtSecretKey.length());
            throw new IllegalStateException("JWT secret key must be at least 32 characters");
        }
        
        // 3. 기본값 체크 (보안상 위험한 기본값 사용 방지)
        if ("your-super-super-long-and-secure-secret-key-for-jwt-hs256".equals(jwtSecretKey)) {
            log.error("기본 JWT 시크릿 키가 감지되었습니다. 이는 보안 위험입니다.");
            throw new IllegalStateException("Default JWT secret key detected. Please use a secure, unique key");
        }
        
        // 4. 약한 패턴 검증
        if (isWeakKey(jwtSecretKey)) {
            log.warn("JWT 시크릿 키가 약해 보입니다. 더 복잡한 키 사용을 고려해주세요.");
        }
        
        log.info("JWT 설정 검증 완료 - 키 길이: {} 자", jwtSecretKey.length());
    }
    
    /**
     * 약한 키 패턴 검사
     */
    private boolean isWeakKey(String key) {
        // 반복 문자 검사
        if (key.matches(".*(.)\\1{3,}.*")) {
            return true;
        }
        
        // 순차적 문자 검사 (예: abcd, 1234)
        for (int i = 0; i < key.length() - 3; i++) {
            if (key.charAt(i) + 1 == key.charAt(i + 1) && 
                key.charAt(i + 1) + 1 == key.charAt(i + 2) && 
                key.charAt(i + 2) + 1 == key.charAt(i + 3)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * JWT 서명용 키 생성
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
    }
    
    // 토큰 만료 시간 상수
    public static final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 15; // 15분
    private static final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7; // 7일

    public long getAccessTokenValiditySeconds() {
        return ACCESS_TOKEN_VALIDITY / 1000;
    }

    /**
     * Access Token 생성 (API 호출용)
     */
    public String generateAccessToken(String email, Integer userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);
        String jti = UUID.randomUUID().toString(); // JWT ID 생성
        
        return Jwts.builder()
                .setId(jti) // JWT ID 설정
                .setSubject(email)
                .claim("userId", userId)
                .claim("tokenType", "access")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성 (토큰 갱신용)
     */
    public String generateRefreshToken(String email, Integer userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);
        String jti = UUID.randomUUID().toString(); // JWT ID 생성
        
        return Jwts.builder()
                .setId(jti) // JWT ID 설정
                .setSubject(email)
                .claim("userId", userId)
                .claim("tokenType", "refresh")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    
    /**
     * JWT 토큰 검증
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 토큰이 유효하면 true, 아니면 false
     */
    @SuppressWarnings("deprecation")
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException e) {
            log.error("JWT 서명이 올바르지 않습니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT 토큰 형식이 올바르지 않습니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원하지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * JWT 토큰에서 Claims 추출
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return Claims 객체 (토큰이 유효하지 않으면 null)
     */
    @SuppressWarnings("deprecation")
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * JWT 토큰에서 이메일 추출
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 이메일 주소 (추출 실패 시 null)
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * JWT 토큰에서 사용자 ID 추출
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 사용자 ID (추출 실패 시 null)
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("userId") != null) {
            // Integer나 Long으로 올 수 있으므로 안전하게 처리
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
        }
        return null;
    }
    
    /**
     * JWT 토큰에서 JTI(JWT ID) 추출
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return JTI (추출 실패 시 null)
     */
    public String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getId() : null;
    }
    
    /**
     * JWT 토큰 만료 시간 추출
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 만료 시간 (추출 실패 시 null)
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }
    
    /**
     * JWT 토큰이 만료되었는지 확인
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
    
    /**
     * JWT 토큰 타입 확인
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 토큰 타입 ("access", "refresh" 또는 null)
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("tokenType") : null;
    }

    /**
     * Access Token인지 확인
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    /**
     * Refresh Token인지 확인
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    /**
     * JWT 토큰 해시 생성 (DB 저장용)
     * @param token 원본 토큰
     * @return SHA-256 해시값
     */
    public String generateTokenHash(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * @param authorizationHeader Authorization 헤더 값
     * @return Bearer 접두사가 제거된 JWT 토큰 (유효하지 않으면 null)
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // "Bearer " 제거
        }
        return null;
    }

    /**
     * JWT 토큰에서 JwtClaims 객체 생성
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return JwtClaims 객체 (토큰이 유효하지 않으면 null)
     */
    public JwtClaims getJwtClaims(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims == null) {
                return null;
            }

            return JwtClaims.builder()
                    .email(claims.getSubject())
                    .userId(extractUserIdFromClaims(claims))
                    .tokenType((String) claims.get("tokenType"))
                    .jti(claims.getId())
                    .expiresAt(convertToLocalDateTime(claims.getExpiration()))
                    .build();
        } catch (Exception e) {
            log.error("JWT Claims 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Authorization 헤더에서 JwtClaims 객체 생성
     * @param authorizationHeader "Bearer {token}" 형식의 헤더
     * @return JwtClaims 객체 (헤더가 유효하지 않으면 null)
     */
    public JwtClaims getJwtClaimsFromHeader(String authorizationHeader) {
        String token = extractTokenFromHeader(authorizationHeader);
        if (token == null) {
            return null;
        }
        return getJwtClaims(token);
    }

    // === Private Helper Methods ===

    /**
     * Claims에서 사용자 ID 안전하게 추출
     */
    private Integer extractUserIdFromClaims(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return (Integer) userIdObj;
        } else if (userIdObj instanceof Long) {
            return ((Long) userIdObj).intValue();
        }
        return null;
    }

    /**
     * Date를 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}