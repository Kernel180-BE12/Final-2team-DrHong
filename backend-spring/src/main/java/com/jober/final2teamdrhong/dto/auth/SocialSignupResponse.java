package com.jober.final2teamdrhong.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 소셜 로그인 회원가입 완료 후 응답 DTO
 */
@Schema(
    description = "소셜 로그인 회원가입 완료 응답 DTO",
    example = """
    {
        "success": true,
        "message": "소셜 회원가입이 성공적으로 완료되었습니다.",
        "userId": 123,
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "refresh_token_here",
        "email": "user@example.com",
        "name": "홍길동",
        "role": "USER",
        "provider": "google"
    }
    """
)
public record SocialSignupResponse(
    @Schema(description = "회원가입 성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 메시지", example = "소셜 회원가입이 성공적으로 완료되었습니다.")
    String message,

    @Schema(description = "생성된 사용자 ID", example = "123")
    Integer userId,

    @Schema(description = "JWT 액세스 토큰")
    String accessToken,

    @Schema(description = "JWT 리프레시 토큰")
    String refreshToken,

    @Schema(description = "사용자 이메일", example = "user@example.com")
    String email,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 역할", example = "USER")
    String role,

    @Schema(description = "OAuth2 제공자", example = "google")
    String provider
) {

    /**
     * 성공 응답 생성
     *
     * @param userId 사용자 ID
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param email 이메일
     * @param name 이름
     * @param role 역할
     * @param provider OAuth2 제공자
     * @return 성공 응답
     */
    public static SocialSignupResponse success(Integer userId, String accessToken, String refreshToken,
                                             String email, String name, String role, String provider) {
        return new SocialSignupResponse(
                true,
                "소셜 회원가입이 성공적으로 완료되었습니다.",
                userId,
                accessToken,
                refreshToken,
                email,
                name,
                role,
                provider
        );
    }

    /**
     * 실패 응답 생성
     *
     * @param message 실패 메시지
     * @return 실패 응답
     */
    public static SocialSignupResponse failure(String message) {
        return new SocialSignupResponse(
                false,
                message,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}