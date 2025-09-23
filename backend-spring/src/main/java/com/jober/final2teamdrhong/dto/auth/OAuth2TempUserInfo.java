package com.jober.final2teamdrhong.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * OAuth2 소셜 로그인 시 임시로 저장되는 사용자 정보 DTO
 * Redis에 저장되어 신규 사용자가 핸드폰 번호를 입력할 때까지 임시 보관됩니다.
 */
@Schema(
    description = "OAuth2 임시 사용자 정보 DTO",
    example = """
    {
        "provider": "google",
        "socialId": "123456789012345678901",
        "email": "user@example.com",
        "name": "홍길동",
        "imageUrl": "https://lh3.googleusercontent.com/a/example",
        "createdAt": 1698765432000
    }
    """
)
public record OAuth2TempUserInfo(
    @Schema(description = "OAuth2 제공자", example = "google")
    String provider,

    @Schema(description = "소셜 로그인 제공자에서의 고유 ID", example = "123456789012345678901")
    String socialId,

    @Schema(description = "사용자 이메일 주소", example = "user@example.com")
    String email,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "프로필 이미지 URL (선택사항)")
    String imageUrl,

    @Schema(description = "임시 키 생성 시간 (디버깅 및 로깅용)", example = "1698765432000")
    Long createdAt
) implements Serializable {

    /**
     * OAuth2UserInfo 객체로부터 OAuth2TempUserInfo 생성
     *
     * @param oAuth2UserInfo OAuth2 사용자 정보
     * @param provider OAuth2 제공자
     * @return OAuth2TempUserInfo 객체
     */
    public static OAuth2TempUserInfo fromOAuth2UserInfo(OAuth2UserInfo oAuth2UserInfo, String provider) {
        return new OAuth2TempUserInfo(
                provider,
                oAuth2UserInfo.getId(),
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getName(),
                oAuth2UserInfo.getImageUrl(),
                System.currentTimeMillis()
        );
    }

    /**
     * 필수 정보가 모두 포함되어 있는지 검증
     *
     * @return 필수 정보 완성 여부
     */
    @JsonIgnore
    public boolean isValid() {
        return provider != null && !provider.trim().isEmpty() &&
               socialId != null && !socialId.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               name != null && !name.trim().isEmpty();
    }

    /**
     * 이메일 도메인 검증 (기본적인 형식 체크)
     *
     * @return 이메일 형식 유효성
     */
    @JsonIgnore
    public boolean isEmailValid() {
        return email != null && email.contains("@") && email.contains(".");
    }
}