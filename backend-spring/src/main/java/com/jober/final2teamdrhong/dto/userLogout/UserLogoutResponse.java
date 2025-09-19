package com.jober.final2teamdrhong.dto.userLogout;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 로그아웃 응답")
public record UserLogoutResponse(
    @Schema(description = "응답 성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 메시지", example = "로그아웃이 성공적으로 완료되었습니다.")
    String message
) {
    public static UserLogoutResponse success(String message) {
        return new UserLogoutResponse(true, message);
    }

    public static UserLogoutResponse error(String message) {
        return new UserLogoutResponse(false, message);
    }
}