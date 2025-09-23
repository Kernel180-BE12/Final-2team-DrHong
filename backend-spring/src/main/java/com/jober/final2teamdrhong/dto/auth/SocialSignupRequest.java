package com.jober.final2teamdrhong.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 소셜 로그인 후 회원가입 완료를 위한 요청 DTO
 * OAuth2 임시 정보와 사용자가 입력한 핸드폰 번호를 결합하여 회원가입을 완료합니다.
 */
@Schema(
    description = "소셜 로그인 회원가입 완료 요청 DTO",
    example = """
    {
        "tempKey": "oauth2_temp:12345678-1234-1234-1234-123456789012",
        "phoneNumber": "010-1234-5678",
        "agreedToTerms": true,
        "agreedToPrivacyPolicy": true,
        "agreedToMarketing": false
    }
    """
)
public record SocialSignupRequest(
    @Schema(description = "Redis에 저장된 OAuth2 임시 정보 키", example = "oauth2_temp:12345678-1234-1234-1234-123456789012")
    @NotBlank(message = "임시 키는 필수입니다.")
    String tempKey,

    @Schema(description = "핸드폰 번호 (010 번호만 가능)", example = "010-1234-5678")
    @NotBlank(message = "핸드폰 번호는 필수입니다.")
    @Pattern(regexp = "^010-[0-9]{4}-[0-9]{4}$",
             message = "핸드폰 번호는 010-XXXX-XXXX 형식이어야 합니다.")
    String phoneNumber,

    @Schema(description = "약관 동의 여부", example = "true")
    @NotNull(message = "약관 동의는 필수입니다.")
    Boolean agreedToTerms,

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    @NotNull(message = "개인정보 처리방침 동의는 필수입니다.")
    Boolean agreedToPrivacyPolicy,

    @Schema(description = "마케팅 정보 수신 동의 여부 (선택사항)", example = "false")
    Boolean agreedToMarketing
) {
    /**
     * 기본값 설정된 생성자
     */
    public SocialSignupRequest {
        if (agreedToMarketing == null) {
            agreedToMarketing = false;
        }
    }

    /**
     * 핸드폰 번호에서 하이픈을 제거하고 정규화
     *
     * @return 정규화된 핸드폰 번호
     */
    public String getNormalizedPhoneNumber() {
        return phoneNumber != null ? phoneNumber.replaceAll("[.-]", "") : null;
    }

    /**
     * 필수 약관 동의 여부 확인
     *
     * @return 필수 약관 동의 완료 여부
     */
    public boolean hasRequiredAgreements() {
        return Boolean.TRUE.equals(agreedToTerms) &&
               Boolean.TRUE.equals(agreedToPrivacyPolicy);
    }
}