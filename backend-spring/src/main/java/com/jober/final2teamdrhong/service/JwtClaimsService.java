package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.JwtClaims.JwtClaims;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * JWT Claims와 사용자 정보를 처리하는 서비스
 * 
 * 기본 사용법:
 * 1. JWT 토큰에서 기본 Claims 추출: jwtConfig.getJwtClaims(token)
 * 2. DB 정보가 필요한 경우: jwtClaimsService.enrichWithUserInfo(claims)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtClaimsService {
    
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    
    /**
     * JWT 토큰에서 Claims를 추출하고 DB에서 사용자 정보로 보완
     * 
     * @param token JWT 토큰 (Bearer 접두사 제거된 상태)
     * @return 사용자 정보가 포함된 JwtClaims
     * @throws AuthenticationException 토큰이 유효하지 않거나 사용자를 찾을 수 없는 경우
     */
    public JwtClaims getEnrichedClaims(String token) {
        // 1. JWT에서 기본 Claims 추출
        JwtClaims basicClaims = jwtConfig.getJwtClaims(token);
        if (basicClaims == null) {
            throw new AuthenticationException("유효하지 않은 토큰입니다");
        }
        
        // 2. DB에서 사용자 정보 조회 및 보완
        return enrichWithUserInfo(basicClaims);
    }
    
    /**
     * Authorization 헤더에서 Claims를 추출하고 DB에서 사용자 정보로 보완
     * 
     * @param authorizationHeader "Bearer {token}" 형식의 헤더
     * @return 사용자 정보가 포함된 JwtClaims
     * @throws AuthenticationException 헤더나 토큰이 유효하지 않은 경우
     */
    public JwtClaims getEnrichedClaimsFromHeader(String authorizationHeader) {
        String token = jwtConfig.extractTokenFromHeader(authorizationHeader);
        if (token == null) {
            throw new AuthenticationException("유효하지 않은 Authorization 헤더입니다");
        }
        return getEnrichedClaims(token);
    }
    
    /**
     * 기본 JWT Claims에 DB의 사용자 정보를 추가
     * 
     * @param basicClaims JWT에서 추출한 기본 Claims
     * @return 사용자 정보가 추가된 JwtClaims
     * @throws AuthenticationException 사용자를 찾을 수 없는 경우
     */
    @Cacheable(value = "userInfo", key = "#basicClaims.email + ':' + #basicClaims.userId")
    public JwtClaims enrichWithUserInfo(JwtClaims basicClaims) {
        if (basicClaims.getEmail() == null || basicClaims.getUserId() == null) {
            throw new AuthenticationException("토큰에 필수 정보가 누락되었습니다");
        }
        
        // DB에서 사용자 정보 조회
        User user = userRepository.findByUserEmail(basicClaims.getEmail())
                .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다"));
        
        // 토큰의 userId와 DB의 userId 일치 확인
        if (!basicClaims.getUserId().equals(user.getUserId())) {
            throw new AuthenticationException("사용자 정보가 일치하지 않습니다");
        }
        
        // 기본 Claims에 DB 정보 추가
        return JwtClaims.builder()
                // JWT 정보
                .email(basicClaims.getEmail())
                .userId(basicClaims.getUserId())
                .tokenType(basicClaims.getTokenType())
                .jti(basicClaims.getJti())
                .expiresAt(basicClaims.getExpiresAt())
                // DB 정보 추가
                .userName(user.getUserName())
                .userRole(user.getUserRole())
                .build();
    }
    
    /**
     * 사용자 정보 캐시 무효화 (사용자 정보 변경 시 호출)
     * 
     * @param email 사용자 이메일
     * @param userId 사용자 ID
     */
    @CacheEvict(value = "userInfo", key = "#email + ':' + #userId")
    public void evictUserInfoCache(String email, Integer userId) {
        log.debug("사용자 정보 캐시 무효화: email={}, userId={}", email, userId);
    }
    
    /**
     * 모든 사용자 정보 캐시 무효화
     */
    @CacheEvict(value = "userInfo", allEntries = true)
    public void evictAllUserInfoCache() {
        log.debug("모든 사용자 정보 캐시 무효화");
    }
}