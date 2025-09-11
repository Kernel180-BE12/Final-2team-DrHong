package com.jober.final2teamdrhong.dto.EmailVerification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
    description = "이메일 인증 코드 요청 DTO",
    example = """
    {
        "email": "user@example.com"
    }
    """
)
@Getter
@NoArgsConstructor
public class EmailRequest {
    
    @Schema(
        description = "인증 코드를 받을 이메일 주소", 
        example = "user@example.com",
        format = "email"
    )
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
