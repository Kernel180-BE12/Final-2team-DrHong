package com.jober.final2teamdrhong.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.UserSignupRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.jdbc.Sql;

/**
 * 핵심 회원가입 통합 테스트
 * 실제 Redis + 원격 MySQL RDS + Gmail SMTP 환경에서 전체 플로우 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:cleanup-test-data.sql")
class UserSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationStorage verificationStorage;

    @Test
    @DisplayName("✅ 회원가입 성공: Redis 저장소 + 원격 MySQL RDS")
    void signup_success_with_redis_and_remote_rds() throws Exception {
        // given: Redis에 인증 코드 저장
        String email = "test@example.com";
        String verificationCode = "123456";
        verificationStorage.save(email, verificationCode);

        UserSignupRequest request = UserSignupRequest.builder()
                .userName("테스트사용자")
                .email(email)
                .password("Password123!")
                .userNumber("010-1234-5678")
                .verificationCode(verificationCode)
                .build();

        // when & then: API 호출 및 응답 검증
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."));

        // then: 원격 MySQL RDS에 사용자 저장 확인
        User savedUser = userRepository.findByUserEmail(email).orElseThrow();
        assertThat(savedUser.getUserName()).isEqualTo("테스트사용자");
        assertThat(savedUser.getUserRole()).isEqualTo(User.UserRole.USER);
        assertThat(savedUser.getUserAuths()).hasSize(1);

        // then: Redis에서 인증 코드 삭제 확인
        assertThat(verificationStorage.find(email)).isEmpty();
    }

    @Test
    @DisplayName("❌ 회원가입 실패: 잘못된 인증 코드")
    void signup_fail_with_invalid_verification_code() throws Exception {
        // given
        String email = "test@example.com";
        verificationStorage.save(email, "123456");

        UserSignupRequest request = UserSignupRequest.builder()
                .userName("테스트사용자")
                .email(email)
                .password("Password123!")
                .userNumber("010-1234-5678")
                .verificationCode("999999") // 잘못된 코드
                .build();

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 코드가 일치하지 않습니다."));

        // then: 데이터베이스에 사용자가 저장되지 않았는지 확인
        assertThat(userRepository.findByUserEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("❌ 회원가입 실패: 중복된 이메일")
    void signup_fail_with_duplicate_email() throws Exception {
        // given: 이미 존재하는 사용자
        User existingUser = User.create("기존사용자", "test@example.com", "010-0000-0000");
        userRepository.save(existingUser);

        verificationStorage.save("test@example.com", "123456");

        UserSignupRequest request = UserSignupRequest.builder()
                .userName("신규사용자")
                .email("test@example.com")
                .password("Password123!")
                .userNumber("010-1234-5678")
                .verificationCode("123456")
                .build();

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    @Test
    @DisplayName("✅ 이메일 인증 코드 발송 성공")
    void send_verification_code_success() throws Exception {
        // given
        String requestJson = "{\"email\":\"test@example.com\"}";

        // when & then
        mockMvc.perform(post("/auth/send-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."));

        // then: Redis에 인증 코드가 저장되었는지 확인
        assertThat(verificationStorage.find("test@example.com")).isPresent();
    }
}