package com.jober.final2teamdrhong.dto.auth;

import com.jober.final2teamdrhong.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * OAuth2 사용자 정보 팩토리 테스트
 * 다양한 OAuth2 제공자의 사용자 정보 생성을 검증합니다.
 */
class OAuth2UserInfoFactoryTest {

    @Test
    @DisplayName("Google OAuth2 사용자 정보 생성 성공")
    void getOAuth2UserInfo_Google_Success() {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456789");
        attributes.put("name", "홍길동");
        attributes.put("email", "hong@gmail.com");
        attributes.put("picture", "https://lh3.googleusercontent.com/a/example");

        // When
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributes);

        // Then
        assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        assertThat(userInfo.getId()).isEqualTo("123456789");
        assertThat(userInfo.getName()).isEqualTo("홍길동");
        assertThat(userInfo.getEmail()).isEqualTo("hong@gmail.com");
        assertThat(userInfo.getImageUrl()).isEqualTo("https://lh3.googleusercontent.com/a/example");
    }

    @Test
    @DisplayName("Google OAuth2 사용자 정보 생성 - 대소문자 구분 없음")
    void getOAuth2UserInfo_GoogleUpperCase_Success() {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "123456789");
        attributes.put("name", "홍길동");
        attributes.put("email", "hong@gmail.com");
        attributes.put("picture", "https://lh3.googleusercontent.com/a/example");

        // When
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("GOOGLE", attributes);

        // Then
        assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        assertThat(userInfo.getEmail()).isEqualTo("hong@gmail.com");
    }

    @Test
    @DisplayName("지원하지 않는 OAuth2 제공자 - 예외 발생")
    void getOAuth2UserInfo_UnsupportedProvider_ThrowsException() {
        // Given
        Map<String, Object> attributes = new HashMap<>();
        String unsupportedProvider = "facebook";

        // When & Then
        assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo(unsupportedProvider, attributes))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 소셜 로그인 제공자입니다: facebook");
    }

    @Test
    @DisplayName("null 제공자 - 예외 발생")
    void getOAuth2UserInfo_NullProvider_ThrowsException() {
        // Given
        Map<String, Object> attributes = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo(null, attributes))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("빈 문자열 제공자 - 예외 발생")
    void getOAuth2UserInfo_EmptyProvider_ThrowsException() {
        // Given
        Map<String, Object> attributes = new HashMap<>();

        // When & Then
        assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo("", attributes))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 소셜 로그인 제공자입니다");
    }
}