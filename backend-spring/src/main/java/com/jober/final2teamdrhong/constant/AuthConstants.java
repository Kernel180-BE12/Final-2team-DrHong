package com.jober.final2teamdrhong.constant;

/**
 * 인증 관련 상수 정의
 */
public class AuthConstants {
    
    // 에러 메시지
    public static final String INVALID_CREDENTIALS = "이메일 또는 비밀번호가 일치하지 않습니다.";
    public static final String INVALID_REFRESH_TOKEN = "유효하지 않은 리프레시 토큰입니다.";
    public static final String EXPIRED_REFRESH_TOKEN = "만료되었거나 유효하지 않은 리프레시 토큰입니다.";
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다.";
    public static final String INVALID_TOKEN_INFO = "토큰 정보가 유효하지 않습니다.";
    
    // 보안 설정
    public static final int MIN_RESPONSE_TIME_MS = 200;
    public static final String DUMMY_HASH = "$2a$10$dummy.hash.to.prevent.timing.attacks.for.security.purposes";
    
    // 토큰 설정
    public static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60; // 7일
    
    // Redis 키 패턴
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    public static final String USER_TOKENS_KEY_PREFIX = "user_tokens:";
    
    private AuthConstants() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}