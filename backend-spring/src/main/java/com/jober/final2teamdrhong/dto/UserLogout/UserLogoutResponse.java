package com.jober.final2teamdrhong.dto.UserLogout;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 로그아웃 응답")
public class UserLogoutResponse {

    @Schema(description = "응답 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "로그아웃이 성공적으로 완료되었습니다.")
    private String message;

    public static UserLogoutResponse success(String message) {
        return UserLogoutResponse.builder().success(true).message(message).build();
    }

    public static UserLogoutResponse error(String message) {
        return UserLogoutResponse.builder().success(false).message(message).build();
    }
}