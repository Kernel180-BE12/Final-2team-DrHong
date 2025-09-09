package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


/**
 * JWT 토큰 블랙리스트 관리 서비스(Redis 전용)
 *
 * 주요 기능 :
 * - 토큰 블랙리스트 추가/제거
 * - 토큰 블랙리스트 조회
 * - 자동 만료 처리 TTL
 * - 사용자별 전체 토큰 무효화
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    //Redis 키 패턴  
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    /**
     * JWT 토큰을 블랙리스트에 추가 (JTI 기반)
     * @param token JWT 토큰
     * @param ttlSeconds TTL(초 단위)
     */
    public void addTokenToBlacklist(String token, long ttlSeconds) {
        try {
            String jti = jwtConfig.getJtiFromToken(token);
            if (jti == null) {
                log.error("토큰에서 JTI를 추출할 수 없습니다.");
                throw new IllegalArgumentException("Cannot extract JTI from token");
            }
            
            String key = BLACKLIST_KEY_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
            log.info("토큰이 블랙리스트에 추가되었습니다. JTI: {}, TTL: {}초", jti, ttlSeconds);
        } catch (Exception e) {
            log.error("토큰 블랙리스트 추가 실패: {}", e.getMessage());
            throw new RuntimeException("Failed to add token to blacklist", e);
        }
    }
    
    /**
     * JTI를 직접 블랙리스트에 추가 (내부용)
     * @param jti JWT ID
     * @param ttlSeconds TTL(초 단위)
     */
    private void addJtiToBlacklist(String jti, long ttlSeconds) {
        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
            log.info("JTI가 블랙리스트에 추가되었습니다. JTI: {}, TTL: {}초", jti, ttlSeconds);
        } catch (Exception e) {
            log.error("JTI 블랙리스트 추가 실패: {}", e.getMessage());
            throw new RuntimeException("Failed to add JTI to blacklist", e);
        }
    }

    /**
     * Access Token을 블랙리스트에 추가 (15분 TTL)
     * @param token JWT Access Token
     */
    public void addAccessTokenToBlacklist(String token) {
        addTokenToBlacklist(token, 15 * 60); // 15분
    }

    /**
     * Refresh Token을 블랙리스트에 추가 (7일 TTL)
     * @param token JWT Refresh Token
     */
    public void addRefreshTokenToBlacklist(String token) {
        addTokenToBlacklist(token, 7 * 24 * 60 * 60); // 7일
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인 (JTI 기반)
     *
     * @param token JWT 토큰
     * @return 블랙리스트에 있으면 true, 아니면 false
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String jti = jwtConfig.getJtiFromToken(token);
            if (jti == null) {
                log.warn("토큰에서 JTI를 추출할 수 없습니다. 블랙리스트로 처리합니다.");
                return true; // JTI 추출 실패 시 보안상 차단
            }
            
            String key = BLACKLIST_KEY_PREFIX + jti;
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("토큰 블랙리스트 확인 실패: {}", e.getMessage());
            // Redis 장애 시 보안을 위해 true를 반환 (Fail-Safe)
            return true;
        }
    }

    /**
     * 토큰을 블랙리스트에서 제거 (JTI 기반)
     * @param token JWT 토큰
     */
    public void removeTokenFromBlacklist(String token) {
        try {
            String jti = jwtConfig.getJtiFromToken(token);
            if (jti == null) {
                log.warn("토큰에서 JTI를 추출할 수 없습니다.");
                return;
            }
            
            String key = BLACKLIST_KEY_PREFIX + jti;
            redisTemplate.delete(key);
            log.info("토큰이 블랙리스트에서 제거되었습니다. JTI: {}", jti);
        } catch (Exception e) {
            log.error("토큰 블랙리스트 제거 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자의 모든 토큰을 블랙리스트에서 제거
     * @param userId 사용자 ID
     */
    public void removeAllUserTokensFromBlacklist(Long userId) {
        try {
            String pattern = BLACKLIST_KEY_PREFIX + "*";
            // 실제로는 사용자별 토큰 관리가 필요하지만, 현재는 패턴 매칭으로 처리
            // 향후 개선: 사용자별 토큰 목록 관리
            log.info("사용자 {}의 토큰 블랙리스트 제거 요청", userId);
        } catch (Exception e) {
            log.error("사용자 토큰 블랙리스트 제거 실패: {}", e.getMessage());
        }
    }

    // 추후 구현? 블랙리스트 통계 조회? 과연 필요할까?
}
