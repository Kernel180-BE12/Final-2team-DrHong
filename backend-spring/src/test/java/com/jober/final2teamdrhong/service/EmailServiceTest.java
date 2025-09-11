package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
<<<<<<< HEAD
import org.springframework.test.context.ActiveProfiles;
=======
>>>>>>> origin/dev

/**
 * EmailService 단위 테스트
 * Mock을 사용하여 실제 이메일 발송 없이 로직만 테스트
<<<<<<< HEAD
 * H2 인메모리 DB 사용
=======
>>>>>>> origin/dev
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private VerificationStorage verificationStorage;
    
    @Mock
    private RateLimitService rateLimitService;
    
    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("성공: 유효한 이메일로 인증 코드 발송")
    void sendVerificationCode_success() {
        // given
        String email = "test@example.com";

        // when
        emailService.sendVerificationCode(email);

        // then
        then(verificationStorage).should().save(anyString(), anyString());
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("실패: 빈 이메일로 인증 코드 발송")
    void sendVerificationCode_fail_emptyEmail() {
        // given
        String emptyEmail = "";

        // when & then
        assertThatThrownBy(() -> emailService.sendVerificationCode(emptyEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일을 입력해주세요");

        then(verificationStorage).shouldHaveNoInteractions();
        then(mailSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패: null 이메일로 인증 코드 발송")
    void sendVerificationCode_fail_nullEmail() {
        // given
        String nullEmail = null;

        // when & then
        assertThatThrownBy(() -> emailService.sendVerificationCode(nullEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일을 입력해주세요");

        then(verificationStorage).shouldHaveNoInteractions();
        then(mailSender).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공: Rate limiting과 함께 인증 코드 발송")
    void sendVerificationCodeWithRateLimit_success() {
        // given
        String email = "test@example.com";
        String clientIp = "127.0.0.1";

        // when
        emailService.sendVerificationCodeWithRateLimit(email, clientIp);

        // then
        then(rateLimitService).should().checkEmailSendRateLimit(clientIp, email);
        then(verificationStorage).should().save(anyString(), anyString());
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }
}