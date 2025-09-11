package com.jober.final2teamdrhong.exception;

import com.jober.final2teamdrhong.service.SecurityAuditService;
import com.jober.final2teamdrhong.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * 글로벌 예외 처리기
 * 모든 API에서 발생하는 예외를 통일된 ErrorResponse 형식으로 처리
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    private final SecurityAuditService securityAuditService;
    
    @Value("${app.environment.development:true}")
    private boolean isDevelopment;

    /**
     * @Valid 검증 실패 시 발생하는 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        // 첫 번째 에러 메시지 추출
        List<String> errorMessages = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorMessages.add(error.getDefaultMessage());
        }
        String errorMessage = errorMessages.isEmpty() ? "검증 오류가 발생했습니다." : errorMessages.get(0);
        
        log.warn("Validation failed: path={}, error={}", request.getRequestURI(), errorMessage);
        
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(errorMessage));
    }

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        log.warn("Business logic error: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 인증 관련 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, 
            HttpServletRequest request) {
        
        // 보안 이벤트 로깅
        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);
        String userAgent = request.getHeader("User-Agent");
        
        securityAuditService.logAuthenticationFailure(
            extractEmailFromRequest(request), 
            clientIp, 
            ex.getMessage(), 
            userAgent
        );
        
        log.warn("Authentication failed: path={}, ip={}, error={}", 
                request.getRequestURI(), clientIp, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage()));
    }
    
    /**
     * 중복 리소스 예외 처리 (이메일 중복 등)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, 
            HttpServletRequest request) {
        
        log.warn("Duplicate resource: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }
    
    /**
     * 일반 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, 
            HttpServletRequest request) {
        
        log.warn("Business exception: path={}, error={}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
    }
    
    /**
     * Spring Security BadCredentialsException 처리
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, 
            HttpServletRequest request) {
        
        // 보안 로깅
        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);
        securityAuditService.logAuthenticationFailure(
            extractEmailFromRequest(request), 
            clientIp, 
            "Bad credentials", 
            request.getHeader("User-Agent")
        );
        
        log.warn("Bad credentials: path={}, ip={}", request.getRequestURI(), clientIp);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("이메일 또는 비밀번호가 일치하지 않습니다."));
    }

    /**
     * Rate Limiting 예외 처리
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, 
            HttpServletRequest request) {
        
        log.warn("Rate limit exceeded: path={}, message={}, retry after {} seconds", 
                request.getRequestURI(), ex.getMessage(), ex.getRetryAfterSeconds());
        
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getRetryAfterSeconds());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    /**
     * JSON 파싱 오류 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, 
            HttpServletRequest request) {
        
        log.warn("JSON parsing error: path={}", request.getRequestURI());
        
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("잘못된 요청 형식입니다. JSON 형식을 확인해주세요."));
    }

    /**
     * 모든 나머지 예외 처리 (최후의 보루)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex, 
            HttpServletRequest request) {
        
        // 에러 추적용 고유 ID 생성
        String errorId = String.valueOf(System.currentTimeMillis());
        
        // 상세 에러는 로그에만 기록
        log.error("Unexpected error [{}] at {}: ", errorId, request.getRequestURI(), ex);
        
        String errorMessage = isDevelopment 
            ? String.format("서버 오류: %s (ID: %s)", ex.getMessage(), errorId)
            : String.format("서버 내부 오류가 발생했습니다. (ID: %s)", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errorMessage));
    }
    
    /**
     * 요청에서 이메일 추출 (보안 로깅용)
     */
    private String extractEmailFromRequest(HttpServletRequest request) {
        try {
            // Query parameter에서 이메일 추출 시도
            String queryString = request.getQueryString();
            if (queryString != null && queryString.contains("email=")) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.startsWith("email=")) {
                        return param.substring(6);
                    }
                }
            }
            
            // Path variable에서 추출 시도 (예: /users/{email})
            String path = request.getRequestURI();
            if (path.contains("@")) {
                String[] parts = path.split("/");
                for (String part : parts) {
                    if (part.contains("@")) {
                        return part;
                    }
                }
            }
            
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}