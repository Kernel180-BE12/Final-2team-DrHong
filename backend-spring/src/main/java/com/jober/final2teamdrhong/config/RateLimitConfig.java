package com.jober.final2teamdrhong.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitConfig {

    // =========================================
    // Rate Limiting 기본값 상수
    // =========================================
    public static final class Defaults {
        // 이메일 발송 제한 기본값
        public static final int EMAIL_SEND_REQUESTS_PER_WINDOW = 3;
        public static final int EMAIL_SEND_WINDOW_DURATION_MINUTES = 5;
        
        // 인증 코드 검증 제한 기본값
        public static final int EMAIL_VERIFY_REQUESTS_PER_WINDOW = 5;
        public static final int EMAIL_VERIFY_WINDOW_DURATION_MINUTES = 10;
        
        // 회원가입 제한 기본값
        public static final int SIGNUP_REQUESTS_PER_WINDOW = 10;
        public static final int SIGNUP_WINDOW_DURATION_MINUTES = 60;

        // 로그인 시도 제한 기본값
        public static final int LOGIN_REQUESTS_PER_WINDOW = 5;
        public static final int LOGIN_WINDOW_DURATION_MINUTES = 15;

        // 토큰 갱신 제한 기본값
        public static final int REFRESH_TOKEN_REQUESTS_PER_WINDOW = 10;
        public static final int REFRESH_TOKEN_WINDOW_DURATION_MINUTES = 5;

        private Defaults() {
          // 상수 클래스이므로 인스턴스화 방지
        }
    }
    
    private EmailSend emailSend = new EmailSend();
    private EmailVerify emailVerify = new EmailVerify();
    private Signup signup = new Signup();
    private Login login = new Login();
    private RefreshToken refreshToken = new RefreshToken();


    @Getter
    @Setter
    public static class EmailSend {
        private int requestsPerWindow = Defaults.EMAIL_SEND_REQUESTS_PER_WINDOW;
        private int windowDurationMinutes = Defaults.EMAIL_SEND_WINDOW_DURATION_MINUTES;
    }
    
    @Getter
    @Setter
    public static class EmailVerify {
        private int requestsPerWindow = Defaults.EMAIL_VERIFY_REQUESTS_PER_WINDOW;
        private int windowDurationMinutes = Defaults.EMAIL_VERIFY_WINDOW_DURATION_MINUTES;
    }
    
    @Getter
    @Setter
    public static class Signup {
        private int requestsPerWindow = Defaults.SIGNUP_REQUESTS_PER_WINDOW;
        private int windowDurationMinutes = Defaults.SIGNUP_WINDOW_DURATION_MINUTES;
    }

    @Getter
    @Setter
    public static class Login {
        private int requestsPerWindow = Defaults.LOGIN_REQUESTS_PER_WINDOW;
        private int windowDurationMinutes = Defaults.LOGIN_WINDOW_DURATION_MINUTES;
    }

    @Getter
    @Setter
    public static class RefreshToken {
        private int requestsPerWindow = Defaults.REFRESH_TOKEN_REQUESTS_PER_WINDOW;
        private int windowDurationMinutes = Defaults.REFRESH_TOKEN_WINDOW_DURATION_MINUTES;
    }
}