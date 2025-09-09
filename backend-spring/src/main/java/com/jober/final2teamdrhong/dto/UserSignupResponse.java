package com.jober.final2teamdrhong.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "회원가입 및 이메일 인증 응답 DTO")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSignupResponse {
    
    @Schema(description = "성공 여부", example = "true")
    private final boolean success;
    
    @Schema(description = "응답 메시지", example = "회원가입이 성공적으로 완료되었습니다.")
    private final String message;
    
    @Schema(description = "추가 데이터 (필요시)", example = "null")
    private final Object data;
    
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