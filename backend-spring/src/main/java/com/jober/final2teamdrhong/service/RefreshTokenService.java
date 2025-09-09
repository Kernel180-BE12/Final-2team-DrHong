package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.util.Set;

/**
 * RefreshToken 관리 서비스
 * 
 * 주요 기능:
 * - Refresh Token 생성, 검증, 갱신
 * - 토큰 회전 (Token Rotation) 보안 정책
 * - 다중 세션 관리 및 제한
 * - 토큰 정리 및 통계
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {


    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final RateLimitService rateLimitService;
    private final RedisTemplate<String,String> redisTemplate;

    // Redis 키 패턴
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_KEY_PREFIX = "user_tokens:";

    // 설정 상수
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60; //7dlf
    /**
     * 사용자용 Refresh Token 생성 및 저장 (Redis)
     * 기존 토큰이 있으면 새로 교체 (단일 세션 정책)
     */
    public String createRefreshToken(User user, String clientIp) {
        log.info("Refresh Token 생성 시작: userId={}, ip={}", 
                LogMaskingUtil.maskUserId(user.getUserId().longValue()), 
                LogMaskingUtil.maskIpAddress(clientIp));

        // 1. 기존 토큰 무효화 (단일 세션 정책)
        revokeAllUserTokens(user.getUserId().longValue());

        // 2. 새로운 Refresh Token 생성
        String refreshToken = jwtConfig.generateRefreshToken(user.getUserEmail(), user.getUserId().longValue());
        String tokenHash = jwtConfig.generateTokenHash(refreshToken);

        // 3. Redis에 저장
        saveRefreshTokenToRedis(tokenHash,user.getUserId().longValue(),REFRESH_TOKEN_TTL_SECONDS);


        log.info("Refresh Token 생성 완료: tokenId={}, userId={}",
                LogMaskingUtil.maskUserId(user.getUserId().longValue()));

        return refreshToken;
    }

    /**
     * Refresh Token으로 새로운 Access Token + Refresh Token 발급
     * 토큰 회전(Token Rotation) 보안 정책 적용
     */
    public TokenPair refreshTokens(String refreshToken, String clientIp) {
        log.info("토큰 갱신 요청: ip={}", LogMaskingUtil.maskIpAddress(clientIp));

        // 1. Rate Limiting 체크
        rateLimitService.checkLoginRateLimit(clientIp); // 로그인과 동일한 제한 적용

        // 2. Refresh Token 검증
        if (!jwtConfig.validateToken(refreshToken) || !jwtConfig.isRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            throw new AuthenticationException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 3. 토큰에서 사용자 정보 추출
        String email = jwtConfig.getEmailFromToken(refreshToken);
        Long userId = jwtConfig.getUserIdFromToken(refreshToken);
        
        if (email == null || userId == null) {
            log.warn("토큰에서 사용자 정보 추출 실패: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            throw new AuthenticationException("토큰 정보가 유효하지 않습니다.");
        }

        // 4. Redis에서 토큰 검증
        String tokenHash = jwtConfig.generateTokenHash(refreshToken);
        if (!isValidRefreshTokenInRedis(tokenHash)) {
            log.warn("유효하지 않은 Refresh Token: userId={}", userId);
            throw new AuthenticationException("만료되었거나 유효하지 않은 리프레시 토큰입니다.");
        }
        // 5. DB에서 사용자 조회
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다."));

        // 6. 새로운 토큰 쌍 생성
        String newAccessToken = jwtConfig.generateAccessToken(email, userId);
        String newRefreshToken = jwtConfig.generateRefreshToken(email, userId);
        String newTokenHash = jwtConfig.generateTokenHash(newRefreshToken);

        // 7. 기존 토큰 무효화 및 새 토큰 저장 (Token Rotation)
        revokeRefreshToken(refreshToken);
        saveRefreshTokenToRedis(newTokenHash, userId, REFRESH_TOKEN_TTL_SECONDS);

        log.info("토큰 갱신 완료: userId={}", LogMaskingUtil.maskUserId(userId));

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Redis에 Refresh Token 저장
     * @param tokenHash
     * @param userId
     * @param ttlSeconds
     */
    private void saveRefreshTokenToRedis(String tokenHash, Long userId, long ttlSeconds) {
        try{
            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + tokenHash;
            String userKey = USER_TOKENS_KEY_PREFIX +":" + tokenHash;

            // 토큰 해시 -> 사용자 ID 매핑 저장
            redisTemplate.opsForValue().set(tokenKey,userId.toString(), Duration.ofSeconds(ttlSeconds));

            // 사용자 별 토큰 목록에 추가 (정리용)
            redisTemplate.opsForSet().add(userKey, tokenHash);
            redisTemplate.expire(userKey, Duration.ofSeconds(ttlSeconds));

            log.debug("Refresh Token이 Redis에 저장되었습니다. userId = {}, TTL = {} 초", userId, ttlSeconds);
        }catch (Exception e){
            log.error("Redis Refresh Token 저장 실패 : {}", e.getMessage());
            throw new RuntimeException("Failed to save Refresh Token",e);
        }
    }


    /**
     * Redis에서 Refresh Token 검증
     */
    private boolean isValidRefreshTokenInRedis(String tokenHash) {
        try {
            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + tokenHash;
            Boolean exists = redisTemplate.hasKey(tokenKey);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Redis Refresh Token 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 특정 Refresh Token 무효화
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        String tokenHash = jwtConfig.generateTokenHash(refreshToken);

        try {
            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + tokenHash;
            String userIdStr = redisTemplate.opsForValue().get(tokenKey);

            if (userIdStr != null) {
                Long userId = Long.valueOf(userIdStr);
                String userKey = USER_TOKENS_KEY_PREFIX + userId + ":" + tokenHash;

                // 토큰 삭제
                redisTemplate.delete(tokenKey);
                // 사용자별 토큰 목록에서도 삭제
                redisTemplate.opsForSet().remove(userKey, tokenHash);

                log.info("Refresh Token 무효화 완료: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Redis Refresh Token 무효화 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자의 모든 Refresh Token 무효화 (전체 로그아웃)
     */
    public void revokeAllUserTokens(Long userId) {
        try {
            String userPattern = USER_TOKENS_KEY_PREFIX + userId + ":*";
            Set<String> userKeys = redisTemplate.keys(userPattern);

            if (userKeys != null && !userKeys.isEmpty()) {
                // 사용자별 토큰 목록에서 모든 토큰 해시 조회
                for (String userKey : userKeys) {
                    Set<String> tokenHashes = redisTemplate.opsForSet().members(userKey);
                    if (tokenHashes != null) {
                        for (String tokenHash : tokenHashes) {
                            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + tokenHash;
                            redisTemplate.delete(tokenKey);
                        }
                    }
                    redisTemplate.delete(userKey);
                }
            }

            log.info("사용자 {}의 모든 Refresh Token이 무효화되었습니다.", userId);
        } catch (Exception e) {
            log.error("Redis 사용자 토큰 무효화 실패: {}", e.getMessage());
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isValidRefreshToken(String refreshToken) {
        if (refreshToken == null || !jwtConfig.validateToken(refreshToken) || !jwtConfig.isRefreshToken(refreshToken)) {
            return false;
        }

        String tokenHash = jwtConfig.generateTokenHash(refreshToken);
        return isValidRefreshTokenInRedis(tokenHash);
    }

    /**
     * 토큰 쌍 DTO
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}