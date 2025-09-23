package com.jober.final2teamdrhong.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 설정 프로퍼티
 * application.properties에서 값을 읽어옴
 */
@Configuration
@ConfigurationProperties(prefix = "auth")
@Getter
@Setter
public class AuthProperties {

    /**
     * 보안 설정
     */
    private Security security = new Security();
    
    /**
     * 토큰 설정
     */
    private Token token = new Token();
    
    /**
     * Redis 키 설정
     */
    private Redis redis = new Redis();
    
    /**
     * 에러 메시지 설정
     */
    private Messages messages = new Messages();

    /**
     * OAuth2 설정
     */
    private OAuth2 oauth2 = new OAuth2();

    @Getter
    @Setter
    public static class Security {
        /**
         * 최소 응답 시간 (밀리초) - 타이밍 공격 방지
         */
        private int minResponseTimeMs = 200;
        
        /**
         * 더미 해시 - 사용자 열거 공격 방지
         */
        private String dummyHash = "$2a$10$dummy.hash.to.prevent.timing.attacks.for.security.purposes";
        
        /**
         * 최대 로그인 시도 횟수
         */
        private int maxLoginAttempts = 5;
        
        /**
         * 계정 잠금 시간 (분)
         */
        private int accountLockDurationMinutes = 30;
    }

    @Getter
    @Setter
    public static class Token {
        /**
         * Access Token 유효 시간 (초)
         */
        private long accessTokenValiditySeconds;
        
        /**
         * Refresh Token 유효 시간 (초)
         */
        private long refreshTokenValiditySeconds;
        
        /**
         * Token 갱신 가능 시간 (초) - 만료 전 이 시간 내에서만 갱신 가능
         */
        private long refreshThresholdSeconds;
    }

    @Getter
    @Setter
    public static class Redis {
        /**
         * Refresh Token Redis 키 접두사
         */
        private String refreshTokenKeyPrefix = "refresh_token:";
        
        /**
         * 사용자 토큰 목록 Redis 키 접두사
         */
        private String userTokensKeyPrefix = "user_tokens:";
        
        /**
         * 이메일 인증 코드 Redis 키 접두사
         */
        private String emailVerificationKeyPrefix = "email_verification:";
        
        /**
         * JWT 블랙리스트 Redis 키 접두사
         */
        private String jwtBlacklistKeyPrefix = "jwt:blacklist:";
    }

    @Getter
    @Setter
    public static class Messages {
        /**
         * 잘못된 인증 정보 메시지
         */
        private String invalidCredentials = "이메일 또는 비밀번호가 일치하지 않습니다.";
        
        /**
         * 잘못된 리프레시 토큰 메시지
         */
        private String invalidRefreshToken = "유효하지 않은 리프레시 토큰입니다.";
        
        /**
         * 만료된 리프레시 토큰 메시지
         */
        private String expiredRefreshToken = "만료되었거나 유효하지 않은 리프레시 토큰입니다.";
        
        /**
         * 사용자를 찾을 수 없음 메시지
         */
        private String userNotFound = "사용자를 찾을 수 없습니다.";
        
        /**
         * 잘못된 토큰 정보 메시지
         */
        private String invalidTokenInfo = "토큰 정보가 유효하지 않습니다.";
        
        /**
         * 계정 잠금 메시지
         */
        private String accountLocked = "너무 많은 로그인 시도로 인해 계정이 일시적으로 잠겼습니다.";
        
        /**
         * 인증 코드 만료 메시지
         */
        private String verificationCodeExpired = "인증 코드가 만료되었거나 유효하지 않습니다.";
    }

    @Getter
    @Setter
    public static class OAuth2 {
        /**
         * OAuth2 임시 정보 만료 시간 (분)
         */
        private int tempInfoExpiryMinutes = 15;

        /**
         * OAuth2 임시 정보 만료 시간 연장 최대 제한 (분)
         */
        private int maxExtensionMinutes = 30;

        /**
         * OAuth2 임시 정보 Redis 키 접두사
         */
        private String tempKeyPrefix = "oauth2_temp:";
    }
}