package com.jober.final2teamdrhong.dto.userSignup;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 DTO")
public record UserSignupResponse(
    @Schema(description = "성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 메시지", example = "회원가입이 성공적으로 완료되었습니다.")
    String message,

    @Schema(description = "추가 데이터 (필요시)", example = "null")
    Object data
) {
    // 성공 응답
    public static UserSignupResponse success(String message) {
        return new UserSignupResponse(true, message, null);
    }

    public static UserSignupResponse success(String message, Object data) {
        return new UserSignupResponse(true, message, data);
    }

    // 실패 응답
    public static UserSignupResponse failure(String message) {
        return new UserSignupResponse(false, message, null);
    }
}