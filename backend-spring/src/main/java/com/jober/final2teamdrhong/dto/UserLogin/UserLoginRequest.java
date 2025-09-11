package com.jober.final2teamdrhong.dto.UserLogin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
    description = "로컬 로그인 요청 DTO",
    example = """
    {
        "email": "user@example.com",
        "password": "Password123!"
    }
    """
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginRequest {

    @Schema(
        description = "로그인용 이메일 주소", 
        example = "user@example.com", 
        format = "email"
    )
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @Schema(
        description = "로그인용 비밀번호", 
        example = "Password123!", 
        minLength = 6,
        maxLength = 20
    )
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
    private String password;
}
