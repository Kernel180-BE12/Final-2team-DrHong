package com.jober.final2teamdrhong.dto.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * OAuth2 임시 사용자 정보 DTO 테스트
 * Record 클래스의 직렬화/역직렬화 및 유효성 검증을 테스트합니다.
 */
class OAuth2TempUserInfoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("유효한 OAuth2 임시 사용자 정보 생성")
    void createValidOAuth2TempUserInfo() {
        // When
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // Then
        assertThat(userInfo.provider()).isEqualTo("google");
        assertThat(userInfo.socialId()).isEqualTo("123456789");
        assertThat(userInfo.email()).isEqualTo("test@example.com");
        assertThat(userInfo.name()).isEqualTo("테스트 사용자");
        assertThat(userInfo.imageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(userInfo.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("fromOAuth2UserInfo 팩토리 메서드 테스트")
    void fromOAuth2UserInfo_ValidInput_Success() {
        // Given
        OAuth2UserInfo oAuth2UserInfo = new GoogleOAuth2UserInfo(
                java.util.Map.of(
                        "sub", "123456789",
                        "name", "테스트 사용자",
                        "email", "test@example.com",
                        "picture", "https://example.com/profile.jpg"
                )
        );

        // When
        OAuth2TempUserInfo tempUserInfo = OAuth2TempUserInfo.fromOAuth2UserInfo(oAuth2UserInfo, "google");

        // Then
        assertThat(tempUserInfo.provider()).isEqualTo("google");
        assertThat(tempUserInfo.socialId()).isEqualTo("123456789");
        assertThat(tempUserInfo.email()).isEqualTo("test@example.com");
        assertThat(tempUserInfo.name()).isEqualTo("테스트 사용자");
        assertThat(tempUserInfo.imageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(tempUserInfo.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("유효성 검증 - 모든 필수 필드가 있을 때 true")
    void isValid_AllRequiredFieldsPresent_ReturnsTrue() {
        // Given
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThat(userInfo.isValid()).isTrue();
    }

    @Test
    @DisplayName("유효성 검증 - provider가 null일 때 false")
    void isValid_NullProvider_ReturnsFalse() {
        // Given
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                null,
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThat(userInfo.isValid()).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 - provider가 빈 문자열일 때 false")
    void isValid_EmptyProvider_ReturnsFalse() {
        // Given
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                "",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThat(userInfo.isValid()).isFalse();
    }

    @Test
    @DisplayName("유효성 검증 - email이 null일 때 false")
    void isValid_NullEmail_ReturnsFalse() {
        // Given
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                null,
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThat(userInfo.isValid()).isFalse();
    }

    @Test
    @DisplayName("이메일 유효성 검증 - 유효한 이메일")
    void isEmailValid_ValidEmail_ReturnsTrue() {
        // Given
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThat(userInfo.isEmailValid()).isTrue();
    }

    @Test
    @DisplayName("이메일 유효성 검증 - 무효한 이메일")
    void isEmailValid_InvalidEmail_ReturnsFalse() {
        // Given
        OAuth2TempUserInfo userInfo = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "invalid-email",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                System.currentTimeMillis()
        );

        // When & Then
        assertThat(userInfo.isEmailValid()).isFalse();
    }

    @Test
    @DisplayName("JSON 직렬화/역직렬화 테스트")
    void jsonSerializationDeserialization() throws Exception {
        // Given
        OAuth2TempUserInfo original = new OAuth2TempUserInfo(
                "google",
                "123456789",
                "test@example.com",
                "테스트 사용자",
                "https://example.com/profile.jpg",
                1698765432000L
        );

        // When - 직렬화
        String json = objectMapper.writeValueAsString(original);

        // Then - JSON에 @JsonIgnore 메서드들이 포함되지 않았는지 확인
        assertThat(json).doesNotContain("\"valid\"");
        assertThat(json).doesNotContain("\"emailValid\"");

        // When - 역직렬화
        OAuth2TempUserInfo deserialized = objectMapper.readValue(json, OAuth2TempUserInfo.class);

        // Then
        assertThat(deserialized.provider()).isEqualTo(original.provider());
        assertThat(deserialized.socialId()).isEqualTo(original.socialId());
        assertThat(deserialized.email()).isEqualTo(original.email());
        assertThat(deserialized.name()).isEqualTo(original.name());
        assertThat(deserialized.imageUrl()).isEqualTo(original.imageUrl());
        assertThat(deserialized.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    @DisplayName("JSON 역직렬화 시 알 수 없는 필드 무시")
    void jsonDeserializationWithUnknownFields() throws Exception {
        // Given - valid 필드가 포함된 JSON (과거에 저장된 데이터 시뮬레이션)
        String jsonWithExtraFields = """
                {
                    "provider": "google",
                    "socialId": "123456789",
                    "email": "test@example.com",
                    "name": "테스트 사용자",
                    "imageUrl": "https://example.com/profile.jpg",
                    "createdAt": 1698765432000,
                    "valid": true,
                    "emailValid": true,
                    "unknownField": "should be ignored"
                }
                """;

        // When - @JsonIgnore가 제대로 작동하는지 확인
        OAuth2TempUserInfo deserialized = objectMapper.readValue(jsonWithExtraFields, OAuth2TempUserInfo.class);

        // Then
        assertThat(deserialized.provider()).isEqualTo("google");
        assertThat(deserialized.email()).isEqualTo("test@example.com");
        assertThat(deserialized.isValid()).isTrue(); // 메서드는 정상 작동
        assertThat(deserialized.isEmailValid()).isTrue(); // 메서드는 정상 작동
    }
}