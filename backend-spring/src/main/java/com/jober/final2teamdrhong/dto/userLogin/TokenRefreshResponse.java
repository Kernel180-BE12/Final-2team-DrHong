package com.jober.final2teamdrhong.dto.userLogin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 응답")
public record TokenRefreshResponse(
    @Schema(description = "새로운 Access Token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    String accessToken,

    @Schema(description = "새로운 Refresh Token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    String refreshToken,

    @Schema(description = "토큰 타입", example = "Bearer")
    String tokenType,

    @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
    long expiresIn
) {
    public static TokenRefreshResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenRefreshResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}