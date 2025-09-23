package com.jober.final2teamdrhong.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 탈퇴 요청 DTO
 * 사용자가 계정 탈퇴 시 탈퇴 사유를 입력하도록 합니다.
 */
@Schema(
        description = "회원 탈퇴 요청 DTO",
        example = """
        {
            "reason": "더 이상 서비스를 이용하지 않음",
            "feedback": "서비스가 유용했지만 현재 필요하지 않습니다."
        }
        """
)
public record UserDeactivateRequest(

        @Schema(
                description = "탈퇴 사유 (필수)",
                example = "더 이상 서비스를 이용하지 않음",
                allowableValues = {
                        "더 이상 서비스를 이용하지 않음",
                        "개인정보 보호 우려",
                        "서비스 품질 불만족",
                        "다른 서비스 이용",
                        "기타"
                }
        )
        @NotBlank(message = "탈퇴 사유는 필수입니다.")
        @Size(max = 100, message = "탈퇴 사유는 100자 이내로 입력해주세요.")
        String reason,

        @Schema(
                description = "추가 피드백 (선택사항)",
                example = "서비스가 유용했지만 현재 필요하지 않습니다."
        )
        @Size(max = 500, message = "피드백은 500자 이내로 입력해주세요.")
        String feedback
) {
}