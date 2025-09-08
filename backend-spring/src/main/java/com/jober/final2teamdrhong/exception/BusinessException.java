package com.jober.final2teamdrhong.exception;

/**
 * 일반적인 비즈니스 로직 예외
 * 인증 코드 만료, 업무 규칙 위반 등에 사용
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}