package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * OAuth2 임시 저장 서비스 테스트
 * Redis 연동 및 임시 데이터 관리 기능을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class OAuth2TempStorageServiceTest {

    @Autowired
    private OAuth2TempStorageService oAuth2TempStorageService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private OAuth2TempUserInfo validTempUserInfo;

    @BeforeEach
    void setUp() {
        // 테스트용 유효한 임시 사용자 정보 생성
        validTempUserInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // Redis 테스트 데이터 정리
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("유효한 임시 사용자 정보 저장 성공")
    void storeTempUserInfo_ValidInfo_Success() {
        // When
        String tempKey = oAuth2TempStorageService.storeTempUserInfo(validTempUserInfo);

        // Then
        assertThat(tempKey).isNotNull();
        assertThat(tempKey).startsWith("oauth2_temp:");
        assertThat(tempKey.length()).isGreaterThan("oauth2_temp:".length());

        // Redis에 저장 확인
        Object stored = redisTemplate.opsForValue().get(tempKey);
        assertThat(stored).isNotNull();
    }

    @Test
    @DisplayName("유효하지 않은 임시 사용자 정보 저장 실패")
    void storeTempUserInfo_InvalidInfo_ThrowsException() {
        // Given - 필수 필드가 null인 무효한 정보
        OAuth2TempUserInfo invalidInfo = new OAuth2TempUserInfo(
                null, // provider null
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThatThrownBy(() -> oAuth2TempStorageService.storeTempUserInfo(invalidInfo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OAuth2 임시 사용자 정보가 유효하지 않습니다");
    }

    @Test
    @DisplayName("임시 사용자 정보 조회 성공")
    void retrieveTempUserInfo_ValidKey_Success() {
        // Given
        String tempKey = oAuth2TempStorageService.storeTempUserInfo(validTempUserInfo);

        // When
        OAuth2TempUserInfo retrieved = oAuth2TempStorageService.retrieveTempUserInfo(tempKey);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.email()).isEqualTo(validTempUserInfo.email());
        assertThat(retrieved.name()).isEqualTo(validTempUserInfo.name());
        assertThat(retrieved.provider()).isEqualTo(validTempUserInfo.provider());
        assertThat(retrieved.socialId()).isEqualTo(validTempUserInfo.socialId());
    }

    @Test
    @DisplayName("존재하지 않는 키로 조회 시 null 반환")
    void retrieveTempUserInfo_NonExistentKey_ReturnsNull() {
        // Given
        String nonExistentKey = "oauth2_temp:non-existent-key";

        // When
        OAuth2TempUserInfo retrieved = oAuth2TempStorageService.retrieveTempUserInfo(nonExistentKey);

        // Then
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 키 형식으로 조회 시 예외 발생")
    void retrieveTempUserInfo_InvalidKeyFormat_ThrowsException() {
        // Given
        String invalidKey = "invalid-key-format";

        // When & Then
        assertThatThrownBy(() -> oAuth2TempStorageService.retrieveTempUserInfo(invalidKey))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 임시 키 형식입니다");
    }

    @Test
    @DisplayName("임시 사용자 정보 삭제 성공")
    void deleteTempUserInfo_ValidKey_Success() {
        // Given
        String tempKey = oAuth2TempStorageService.storeTempUserInfo(validTempUserInfo);

        // When
        boolean deleted = oAuth2TempStorageService.deleteTempUserInfo(tempKey);

        // Then
        assertThat(deleted).isTrue();

        // 삭제 확인
        OAuth2TempUserInfo retrieved = oAuth2TempStorageService.retrieveTempUserInfo(tempKey);
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("TTL 조회 성공")
    void getTempUserInfoTTL_ValidKey_ReturnsPositiveValue() {
        // Given
        String tempKey = oAuth2TempStorageService.storeTempUserInfo(validTempUserInfo);

        // When
        long ttl = oAuth2TempStorageService.getTempUserInfoTTL(tempKey);

        // Then
        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(15 * 60); // 15분 이하
    }

    @Test
    @DisplayName("만료 시간 연장 성공")
    void extendTempUserInfo_ValidKey_Success() {
        // Given
        String tempKey = oAuth2TempStorageService.storeTempUserInfo(validTempUserInfo);
        long originalTtl = oAuth2TempStorageService.getTempUserInfoTTL(tempKey);

        // When
        boolean extended = oAuth2TempStorageService.extendTempUserInfo(tempKey, 5);
        long newTtl = oAuth2TempStorageService.getTempUserInfoTTL(tempKey);

        // Then
        assertThat(extended).isTrue();
        assertThat(newTtl).isGreaterThanOrEqualTo(originalTtl);
    }
}