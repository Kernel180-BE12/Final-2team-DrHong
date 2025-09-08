package com.jober.final2teamdrhong.exception;

/**
 * 인증 관련 예외
 * 로그인 실패, 인증 코드 불일치, JWT 검증 실패 등에 사용
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}