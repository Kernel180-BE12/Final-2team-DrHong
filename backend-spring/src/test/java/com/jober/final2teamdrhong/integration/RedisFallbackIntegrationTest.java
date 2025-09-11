package com.jober.final2teamdrhong.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.UserSignupRequest;
import com.jober.final2teamdrhong.dto.UserLoginResponse;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.RefreshTokenService;
import com.jober.final2teamdrhong.service.BlacklistService;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.jober.final2teamdrhong.config.RedisFallbackTestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.jdbc.Sql;

/**
 * Redis 폴백 통합 테스트
 * Redis 비활성화 + 원격 MySQL RDS에서 RDB 폴백 동작 검증
 */

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("redis-fallback-test") // Redis 폴백 테스트 설정 사용
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class) // Redis 비활성화
@Import(RedisFallbackTestConfig.class) // 더미 Redis 설정 임포트
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:cleanup-test-data.sql")
class RedisFallbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationStorage verificationStorage; // RDB 폴백 저장소 (Redis 비활성화시 자동으로 RDB 사용)
    
    @MockBean
    private RefreshTokenService refreshTokenService;
    
    @MockBean
    private BlacklistService blacklistService;

    @Test
    @DisplayName("✅ Redis 폴백: RDB로 자동 전환되어 회원가입 성공")
    void signup_success_with_rdb_fallback() throws Exception {
        // given: Redis 비활성화 상태에서 RDB 폴백 저장소 사용
        String email = "fallback@example.com";
        String verificationCode = "654321";
        verificationStorage.save(email, verificationCode);

        UserSignupRequest request = UserSignupRequest.builder()
                .userName("폴백테스트")
                .email(email)
                .password("Password123!")
                .userNumber("010-9876-5432")
                .verificationCode(verificationCode)
                .build();

        // when & then: API 호출 및 응답 검증
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."));

        // then: 원격 MySQL RDS에 정상 저장 확인
        User savedUser = userRepository.findByUserEmail(email).orElseThrow();
        assertThat(savedUser.getUserName()).isEqualTo("폴백테스트");
        assertThat(savedUser.getUserRole()).isEqualTo(User.UserRole.USER);

        // then: RDB 폴백 저장소에서 인증 코드 삭제 확인
        assertThat(verificationStorage.find(email)).isEmpty();
    }

    @Test
    @DisplayName("❌ Redis 폴백: RDB 폴백 상태에서도 잘못된 인증 코드로 실패")
    void signup_fail_invalid_code_with_rdb_fallback() throws Exception {
        // given
        String email = "fallback@example.com";
        verificationStorage.save(email, "654321");

        UserSignupRequest request = UserSignupRequest.builder()
                .userName("폴백테스트")
                .email(email)
                .password("Password123!")
                .userNumber("010-9876-5432")
                .verificationCode("111111") // 잘못된 코드
                .build();

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증 코드가 일치하지 않거나 만료되었습니다."));

        // then: 데이터베이스에 사용자가 저장되지 않았는지 확인
        assertThat(userRepository.findByUserEmail(email)).isEmpty();
    }
}