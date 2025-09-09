package com.jober.final2teamdrhong;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Final2teamDrHongApplicationTests {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로드되는지 확인
        // Redis + 원격 MySQL RDS + Gmail SMTP 설정 검증
    }
}