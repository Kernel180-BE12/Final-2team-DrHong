package com.jober.final2teamdrhong.exception;

/**
 * 중복 리소스 예외
 * 이메일 중복, 사용자명 중복 등에 사용
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}