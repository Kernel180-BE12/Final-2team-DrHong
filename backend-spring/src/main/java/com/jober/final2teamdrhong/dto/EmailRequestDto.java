package com.jober.final2teamdrhong.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description = "이메일 인증 코드 요청 DTO",
    example = """
    {
        "email": "user@example.com"
    }
    """
)
@Getter
@Setter
@NoArgsConstructor
public class EmailRequestDto {
    
    @Schema(
        description = "인증 코드를 받을 이메일 주소", 
        example = "user@example.com", 
        required = true,
        format = "email",
        pattern = "^[A-Za-z0-9+_.-]+@(.+)$"
    )
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
