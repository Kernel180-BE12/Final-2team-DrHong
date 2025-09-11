package com.jober.final2teamdrhong.integration;

import com.jober.final2teamdrhong.service.EmailService;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
<<<<<<< HEAD
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
>>>>>>> origin/dev
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Gmail SMTP 이메일 발송 통합 테스트
 * ⚠️ 실제 이메일이 발송됩니다!
 */
@SpringBootTest
<<<<<<< HEAD
@ActiveProfiles("integration")
class EmailIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(EmailIntegrationTest.class);

=======
@ActiveProfiles("test")
class EmailIntegrationTest {

>>>>>>> origin/dev
    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationStorage verificationStorage;

    @Test
    @DisplayName("📧 실제 Gmail 자체 발송 테스트 (kernelteam2jdi@gmail.com → 자기 자신)")
    void send_actual_email_to_self() {
        // given
        String testEmail = "kernelteam2jdi@gmail.com"; // 자체 발송

        // when: 실제 이메일 발송
        emailService.sendVerificationCode(testEmail);

        // then: Redis에 인증 코드가 저장되었는지 확인
        assertThat(verificationStorage.find(testEmail)).isPresent();
        
<<<<<<< HEAD
        // 테스트 결과 로깅
        log.info("✅ 실제 이메일이 발송되었습니다!");
        log.info("📧 Gmail 받은편지함을 확인하세요: {}", testEmail);
        log.info("📝 제목: [notimo] 회원가입 이메일 인증 코드입니다.");
=======
        // 콘솔에 안내 메시지 출력
        System.out.println("✅ 실제 이메일이 발송되었습니다!");
        System.out.println("📧 Gmail 받은편지함을 확인하세요: " + testEmail);
        System.out.println("📝 제목: [notimo] 회원가입 이메일 인증 코드입니다.");
>>>>>>> origin/dev
    }

    @Test
    @DisplayName("🔢 6자리 인증 코드 생성 검증")
    void verify_six_digit_code_generation() {
        // given
        String testEmail = "test@example.com";

        // when
        emailService.sendVerificationCode(testEmail);

        // then: 6자리 숫자 코드 확인
        String savedCode = verificationStorage.find(testEmail).orElseThrow();
        assertThat(savedCode).hasSize(6);
        assertThat(savedCode).matches("\\d{6}"); // 6자리 숫자 패턴
        
<<<<<<< HEAD
        log.info("✅ 생성된 6자리 인증 코드: {}", savedCode);
=======
        System.out.println("✅ 생성된 6자리 인증 코드: " + savedCode);
>>>>>>> origin/dev
    }
}