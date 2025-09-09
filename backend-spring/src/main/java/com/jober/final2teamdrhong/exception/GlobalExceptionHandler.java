package com.jober.final2teamdrhong.exception;

import com.jober.final2teamdrhong.dto.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignupResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.ArrayList;
import java.util.List;

/**
 * 글로벌 예외 처리기 - Spring Boot 3.5.5 호환성 개선
 * 
 * Rate Limiting 기능 추가:
 * - RateLimitExceededException: HTTP 429 + Retry-After 헤더
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. @Valid 검증 실패 시 발생하는 예외를 처리하는 메서드
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // 실패한 검증의 첫 번째 에러 메시지를 가져옴
        List<String> errorMessages = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorMessages.add(error.getDefaultMessage());
        }
        String errorMessage = errorMessages.isEmpty() ? "검증 오류가 발생했습니다." : errorMessages.get(0);

        // Auth API인지 확인하여 적절한 응답 타입 반환
        if (isAuthApi(request)) {
            log.warn("Auth API 검증 실패: path={}, error={}", request.getRequestURI(), errorMessage);
            
            // 로그인 API의 경우 UserLoginResponse 사용
            if (request.getRequestURI().contains("/login")) {
                return ResponseEntity.badRequest().body(
                    UserLoginResponse.error(errorMessage)
                );
            }
            
            // 다른 Auth API는 UserSignupResponse 사용
            return ResponseEntity.badRequest().body(
                UserSignupResponse.failure(errorMessage)
            );
        }

        // 다른 API는 기존 ErrorResponse 사용
        return ResponseEntity.badRequest().body(new ErrorResponse(errorMessage));
    }

    // 2. 서비스 계층 등에서 비즈니스 로직상 발생하는 예외를 처리하는 메서드
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        // Auth API인지 확인하여 적절한 응답 타입 반환
        if (isAuthApi(request)) {
            log.warn("Auth API 비즈니스 로직 오류: path={}, error={}", request.getRequestURI(), ex.getMessage());
            return ResponseEntity.badRequest().body(
                UserSignupResponse.failure(ex.getMessage())
            );
        }

        // 다른 API는 기존 ErrorResponse 사용
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    // 3. 인증 관련 예외 처리 (로그인 실패, 인증 코드 불일치 등)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        if (isAuthApi(request)) {
            // 로그인 API의 경우 UserLoginResponse 형태로 에러 응답
            if (request.getRequestURI().contains("/login")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    UserLoginResponse.error(ex.getMessage())
                );
            }
            // 다른 Auth API는 UserSignupResponse 사용
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                UserSignupResponse.failure(ex.getMessage())
            );
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }
    
    // 4. 중복 리소스 예외 처리 (이메일 중복 등)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<?> handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        if (isAuthApi(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                UserSignupResponse.failure(ex.getMessage())
            );
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }
    
    // 5. 일반 비즈니스 예외 처리 (인증 코드 만료, 업무 규칙 위반 등)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business logic error: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        if (isAuthApi(request)) {
            return ResponseEntity.badRequest().body(
                UserSignupResponse.failure(ex.getMessage())
            );
        }
        
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }
    
    // 6. Spring Security BadCredentialsException 처리 (로그인 전용 응답 개선)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        // 로그인 API의 경우 전용 에러 응답 사용
        if (request.getRequestURI().contains("/login")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                UserLoginResponse.error(ex.getMessage())
            );
        }
        
        // 다른 Auth API는 기존 응답 유지
        if (isAuthApi(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                UserSignupResponse.failure(ex.getMessage())
            );
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    // 7. Rate Limiting 예외 처리 - Auth API에서는 UserSignupResponse 반환
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> handleRateLimitExceededException(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: path={}, message={}, retry after {} seconds", 
                request.getRequestURI(), ex.getMessage(), ex.getRetryAfterSeconds());
        
        // Auth API인지 확인하여 적절한 응답 타입 반환
        if (isAuthApi(request)) {
            // 로그인 관련 API의 경우 UserLoginResponse 사용
            if (request.getRequestURI().contains("/login") || 
                request.getRequestURI().contains("/refresh") || 
                request.getRequestURI().contains("/logout")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                        .body(UserLoginResponse.error(ex.getMessage()));
            }
            
            // 다른 Auth API는 UserSignupResponse 사용
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                    .body(UserSignupResponse.failure(ex.getMessage()));
        }

        // 다른 API는 기존 ErrorResponse 사용
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    // 4. JSON 파싱 오류 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("JSON 파싱 오류: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        // Auth API인지 확인하여 적절한 응답 타입 반환
        if (isAuthApi(request)) {
            return ResponseEntity.badRequest().body(
                UserSignupResponse.failure("잘못된 요청 형식입니다.")
            );
        }

        // 다른 API는 기존 ErrorResponse 사용
        return ResponseEntity.badRequest().body(new ErrorResponse("잘못된 요청 형식입니다."));
    }

    // 4. 위에서 처리하지 못한 모든 나머지 예외를 처리하는 최후의 보루
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        // 보안을 위해 상세한 예외 정보는 로그에만 기록하고, 클라이언트에는 일반적인 메시지만 반환
        log.error("Unexpected error occurred at {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        log.debug("Exception details", ex); // 디버그 레벨로 스택트레이스 로깅
        
        ErrorResponse response = new ErrorResponse("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Auth API인지 확인하는 헬퍼 메서드
     */
    private boolean isAuthApi(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/auth/");
    }
}