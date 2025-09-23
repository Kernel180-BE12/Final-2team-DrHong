package com.jober.final2teamdrhong.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 회원 탈퇴 응답 DTO
 * 탈퇴 처리 결과와 관련 정보를 반환합니다.
 */
@Schema(
        description = "회원 탈퇴 응답 DTO",
        example = """
        {
            "success": true,
            "message": "회원 탈퇴가 성공적으로 처리되었습니다.",
            "userId": 123,
            "email": "user@example.com",
            "deactivatedAt": "2024-01-15T10:30:00"
        }
        """
)
public record UserDeactivateResponse(

        @Schema(description = "탈퇴 처리 성공 여부", example = "true")
        boolean success,

        @Schema(description = "응답 메시지", example = "회원 탈퇴가 성공적으로 처리되었습니다.")
        String message,

        @Schema(description = "탈퇴된 사용자 ID", example = "123")
        Integer userId,

        @Schema(description = "탈퇴된 사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "탈퇴 처리 시간", example = "2024-01-15T10:30:00")
        LocalDateTime deactivatedAt
) {

    /**
     * 성공 응답 생성
     */
    public static UserDeactivateResponse success(Integer userId, String email, LocalDateTime deactivatedAt) {
        return new UserDeactivateResponse(
                true,
                "회원 탈퇴가 성공적으로 처리되었습니다.",
                userId,
                email,
                deactivatedAt
        );
    }

    /**
     * 실패 응답 생성
     */
    public static UserDeactivateResponse failure(String message) {
        return new UserDeactivateResponse(
                false,
                message,
                null,
                null,
                null
        );
    }
}