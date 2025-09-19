package com.jober.final2teamdrhong.dto.userLogout;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 로그아웃 요청")
public record UserLogoutRequest(
    @Schema(description = "갱신 토큰", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "Refresh Token은 필수입니다")
    String refreshToken
) {}