package com.jober.final2teamdrhong.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityAuditService 테스트
 * 보안 이벤트 로깅 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SecurityAuditServiceTest {

    @InjectMocks
    private SecurityAuditService securityAuditService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Logback 테스트를 위한 설정
        logger = (Logger) LoggerFactory.getLogger(SecurityAuditService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);
    }

    @Test
    @DisplayName("인증 실패 이벤트 로깅")
    void shouldLogAuthenticationFailure() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        String reason = "Invalid credentials";
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logAuthenticationFailure(email, clientIp, reason, userAgent);

        // then
        assertThat(listAppender.list).hasSize(2); // WARN과 ERROR 로그
        
        ILoggingEvent warnEvent = listAppender.list.get(0);
        assertThat(warnEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnEvent.getFormattedMessage()).contains("SECURITY_EVENT");
        assertThat(warnEvent.getFormattedMessage()).contains("type=AUTH_FAILURE");
        assertThat(warnEvent.getFormattedMessage()).contains("severity=HIGH");
        assertThat(warnEvent.getFormattedMessage()).contains("reason=" + reason);
        
        ILoggingEvent errorEvent = listAppender.list.get(1);
        assertThat(errorEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorEvent.getFormattedMessage()).contains("HIGH_SEVERITY_SECURITY_EVENT");
        assertThat(errorEvent.getFormattedMessage()).contains("AUTH_FAILURE");
    }

    @Test
    @DisplayName("인증 성공 이벤트 로깅")
    void shouldLogAuthenticationSuccess() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        Integer userId = 123;
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logAuthenticationSuccess(email, clientIp, userId, userAgent);

        // then
        assertThat(listAppender.list).hasSize(1); // INFO 레벨이므로 ERROR 로그 없음
        
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("SECURITY_EVENT");
        assertThat(event.getFormattedMessage()).contains("type=AUTH_SUCCESS");
        assertThat(event.getFormattedMessage()).contains("severity=INFO");
        assertThat(event.getFormattedMessage()).contains("userId=");
    }

    @Test
    @DisplayName("계정 잠금 이벤트 로깅")
    void shouldLogAccountLocked() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        int attemptCount = 5;
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logAccountLocked(email, clientIp, attemptCount, userAgent);

        // then
        assertThat(listAppender.list).hasSize(2); // WARN과 ERROR 로그
        
        ILoggingEvent warnEvent = listAppender.list.get(0);
        assertThat(warnEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnEvent.getFormattedMessage()).contains("type=ACCOUNT_LOCKED");
        assertThat(warnEvent.getFormattedMessage()).contains("severity=CRITICAL");
        assertThat(warnEvent.getFormattedMessage()).contains("Too many failed login attempts: " + attemptCount);
        
        ILoggingEvent errorEvent = listAppender.list.get(1);
        assertThat(errorEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorEvent.getFormattedMessage()).contains("HIGH_SEVERITY_SECURITY_EVENT");
    }

    @Test
    @DisplayName("의심스러운 활동 이벤트 로깅")
    void shouldLogSuspiciousActivity() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        String activity = "Multiple rapid login attempts";
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logSuspiciousActivity(email, clientIp, activity, userAgent);

        // then
        assertThat(listAppender.list).hasSize(2); // WARN과 ERROR 로그
        
        ILoggingEvent warnEvent = listAppender.list.get(0);
        assertThat(warnEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnEvent.getFormattedMessage()).contains("type=SUSPICIOUS_ACTIVITY");
        assertThat(warnEvent.getFormattedMessage()).contains("severity=HIGH");
        assertThat(warnEvent.getFormattedMessage()).contains("reason=" + activity);
    }

    @Test
    @DisplayName("Rate Limit 초과 이벤트 로깅")
    void shouldLogRateLimitExceeded() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        String limitType = "login_attempts";
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logRateLimitExceeded(email, clientIp, limitType, userAgent);

        // then
        assertThat(listAppender.list).hasSize(1); // MEDIUM 심각도라 ERROR 로그 없음
        
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("type=RATE_LIMIT_EXCEEDED");
        assertThat(event.getFormattedMessage()).contains("severity=MEDIUM");
        assertThat(event.getFormattedMessage()).contains("Rate limit exceeded: " + limitType);
    }

    @Test
    @DisplayName("토큰 남용 이벤트 로깅")
    void shouldLogTokenAbuse() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        String tokenType = "refresh";
        String reason = "Token reuse detected";
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logTokenAbuse(email, clientIp, tokenType, reason, userAgent);

        // then
        assertThat(listAppender.list).hasSize(2); // WARN과 ERROR 로그
        
        ILoggingEvent warnEvent = listAppender.list.get(0);
        assertThat(warnEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnEvent.getFormattedMessage()).contains("type=TOKEN_ABUSE");
        assertThat(warnEvent.getFormattedMessage()).contains("severity=HIGH");
        assertThat(warnEvent.getFormattedMessage()).contains(tokenType + " token abuse: " + reason);
    }

    @Test
    @DisplayName("이메일 마스킹 확인")
    void shouldMaskEmailInLogs() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        String reason = "Test reason";
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logAuthenticationFailure(email, clientIp, reason, userAgent);

        // then
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getFormattedMessage()).doesNotContain(email); // 원본 이메일은 없어야 함
        assertThat(event.getFormattedMessage()).contains("***"); // 마스킹된 형태여야 함
    }

    @Test
    @DisplayName("IP 주소 마스킹 확인")
    void shouldMaskIpAddressInLogs() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.100";
        String reason = "Test reason";
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logAuthenticationFailure(email, clientIp, reason, userAgent);

        // then
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getFormattedMessage()).doesNotContain("192.168.1.100"); // 원본 IP는 없어야 함
        assertThat(event.getFormattedMessage()).contains("***"); // 마스킹된 형태여야 함
    }

    @Test
    @DisplayName("사용자 ID 마스킹 확인")
    void shouldMaskUserIdInLogs() {
        // given
        String email = "test@example.com";
        String clientIp = "192.168.1.1";
        Integer userId = 12345;
        String userAgent = "Mozilla/5.0";

        // when
        securityAuditService.logAuthenticationSuccess(email, clientIp, userId, userAgent);

        // then
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getFormattedMessage()).doesNotContain("12345"); // 원본 ID는 없어야 함
        assertThat(event.getFormattedMessage()).contains("***"); // 마스킹된 형태여야 함
    }

    @Test
    @DisplayName("null 값 처리 확인")
    void shouldHandleNullValues() {
        // given & when & then
        // 예외가 발생하지 않아야 함
        securityAuditService.logAuthenticationFailure(null, null, null, null);
        securityAuditService.logAuthenticationSuccess(null, null, null, null);
        
        // AUTH_FAILURE는 HIGH severity라서 2번 로깅 (일반 + HIGH_SEVERITY)
        // AUTH_SUCCESS는 INFO severity라서 1번 로깅
        assertThat(listAppender.list).hasSize(3);
        
        // 최소 하나는 SECURITY_EVENT 포함하는지 확인
        boolean hasSecurityEvent = listAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains("SECURITY_EVENT"));
        assertThat(hasSecurityEvent).isTrue();
    }
}