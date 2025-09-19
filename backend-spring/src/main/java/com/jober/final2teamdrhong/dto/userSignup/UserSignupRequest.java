package com.jober.final2teamdrhong.dto.userSignup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
    description = "로컬 회원가입 요청 DTO",
    example = """
    {
        "userName": "홍길동",
        "email": "user@example.com",
        "userNumber": "010-1234-5678",
        "password": "Password123!",
        "verificationCode": "123456"
    }
    """
)
public record UserSignupRequest(
    @Schema(
        description = "사용자 이름",
        example = "홍길동",
        minLength = 2,
        maxLength = 50
    )
    @NotBlank(message = "사용자 이름은 필수입니다.")
    @Size(min = 2 , max = 50 , message = "사용자 이름은 2자 이상 50자 이하여야 합니다.")
    String userName,

    @Schema(
        description = "이메일 주소",
        example = "user@example.com",
        format = "email"
    )
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @Schema(
        description = "핸드폰 번호 (010 번호만 가능)",
        example = "010-1234-5678",
        pattern = "^010-[0-9]{4}-[0-9]{4}$"
    )
    @NotBlank(message = "핸드폰 번호는 필수입니다.")
    @Pattern(regexp = "^010-[0-9]{4}-[0-9]{4}$",
             message = "핸드폰 번호는 010-XXXX-XXXX 형식이어야 합니다.")
    String userNumber,

    @Schema(
        description = "비밀번호 - 보안 요구사항을 만족해야 함",
        example = "Password123!",
        minLength = 6,
        maxLength = 20,
        pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$"
    )
    @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$",
             message = "비밀번호는 6-20자의 대소문자, 숫자, 특수문자를 포함해야 합니다.")
    String password,

    @Schema(
        description = "이메일로 받은 6자리 인증 코드",
        example = "123456",
        pattern = "^[0-9]{6}$",
        minLength = 6,
        maxLength = 6
    )
    @NotBlank(message = "이메일 인증 코드는 필수입니다.")
    String verificationCode
) {}
