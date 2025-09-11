package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.util.LogMaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 보안 이벤트 감사 및 로깅 서비스
 * 보안 관련 이벤트를 구조화된 형태로 로깅하고 모니터링
 */
@Service
@Slf4j
public class SecurityAuditService {

    /**
     * 인증 실패 이벤트 로깅
     */
    public void logAuthenticationFailure(String email, String clientIp, String reason, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType("AUTH_FAILURE")
                .email(email)
                .clientIp(clientIp)
                .reason(reason)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .severity("HIGH")
                .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * 인증 성공 이벤트 로깅
     */
    public void logAuthenticationSuccess(String email, String clientIp, Integer userId, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType("AUTH_SUCCESS")
                .email(email)
                .clientIp(clientIp)
                .userId(userId)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .severity("INFO")
                .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * 계정 잠금 이벤트 로깅
     */
    public void logAccountLocked(String email, String clientIp, int attemptCount, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType("ACCOUNT_LOCKED")
                .email(email)
                .clientIp(clientIp)
                .reason("Too many failed login attempts: " + attemptCount)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .severity("CRITICAL")
                .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * 의심스러운 활동 이벤트 로깅
     */
    public void logSuspiciousActivity(String email, String clientIp, String activity, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType("SUSPICIOUS_ACTIVITY")
                .email(email)
                .clientIp(clientIp)
                .reason(activity)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .severity("HIGH")
                .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Rate Limit 초과 이벤트 로깅
     */
    public void logRateLimitExceeded(String email, String clientIp, String limitType, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType("RATE_LIMIT_EXCEEDED")
                .email(email)
                .clientIp(clientIp)
                .reason("Rate limit exceeded: " + limitType)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .severity("MEDIUM")
                .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * 토큰 남용 이벤트 로깅
     */
    public void logTokenAbuse(String email, String clientIp, String tokenType, String reason, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType("TOKEN_ABUSE")
                .email(email)
                .clientIp(clientIp)
                .reason(String.format("%s token abuse: %s", tokenType, reason))
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .severity("HIGH")
                .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * 구조화된 보안 이벤트 로깅
     */
    private void logSecurityEvent(SecurityEvent event) {
        String maskedEmail = LogMaskingUtil.maskEmail(event.getEmail());
        String maskedIp = LogMaskingUtil.maskIpAddress(event.getClientIp());
        String maskedUserId = event.getUserId() != null ? 
            LogMaskingUtil.maskUserId(event.getUserId().longValue()) : "N/A";
        
        // 구조화된 로그 출력
        log.warn("SECURITY_EVENT | type={} | severity={} | email={} | ip={} | userId={} | reason={} | userAgent={} | timestamp={}", 
            event.getEventType(),
            event.getSeverity(),
            maskedEmail,
            maskedIp,
            maskedUserId,
            event.getReason(),
            event.getUserAgent(),
            event.getTimestamp()
        );
        
        // 높은 심각도 이벤트는 별도 알림 (실제 환경에서는 모니터링 시스템 연동)
        if ("CRITICAL".equals(event.getSeverity()) || "HIGH".equals(event.getSeverity())) {
            log.error("HIGH_SEVERITY_SECURITY_EVENT | {} | {} | {}", 
                event.getEventType(), maskedEmail, event.getReason());
        }
    }
    
    /**
     * 보안 이벤트 데이터 클래스
     */
    private static class SecurityEvent {
        private final String eventType;
        private final String email;
        private final String clientIp;
        private final Integer userId;
        private final String reason;
        private final String userAgent;
        private final LocalDateTime timestamp;
        private final String severity;
        
        private SecurityEvent(Builder builder) {
            this.eventType = builder.eventType;
            this.email = builder.email;
            this.clientIp = builder.clientIp;
            this.userId = builder.userId;
            this.reason = builder.reason;
            this.userAgent = builder.userAgent;
            this.timestamp = builder.timestamp;
            this.severity = builder.severity;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String getEventType() { return eventType; }
        public String getEmail() { return email; }
        public String getClientIp() { return clientIp; }
        public Integer getUserId() { return userId; }
        public String getReason() { return reason; }
        public String getUserAgent() { return userAgent; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSeverity() { return severity; }
        
        public static class Builder {
            private String eventType;
            private String email;
            private String clientIp;
            private Integer userId;
            private String reason;
            private String userAgent;
            private LocalDateTime timestamp;
            private String severity;
            
            public Builder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }
            
            public Builder email(String email) {
                this.email = email;
                return this;
            }
            
            public Builder clientIp(String clientIp) {
                this.clientIp = clientIp;
                return this;
            }
            
            public Builder userId(Integer userId) {
                this.userId = userId;
                return this;
            }
            
            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }
            
            public Builder userAgent(String userAgent) {
                this.userAgent = userAgent;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder severity(String severity) {
                this.severity = severity;
                return this;
            }
            
            public SecurityEvent build() {
                return new SecurityEvent(this);
            }
        }
    }
}