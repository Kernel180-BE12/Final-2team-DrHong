package com.jober.final2teamdrhong.dto.UserLogin;

import com.jober.final2teamdrhong.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "로컬 로그인 응답 DTO")
@Getter
public class UserLoginResponse {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String token;
    
    @Schema(description = "JWT 리프레시 토큰 (선택적)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String refreshToken;
    
    @Schema(description = "토큰 타입", example = "Bearer")
    private final String tokenType;
    
    @Schema(description = "사용자 이름", example = "홍길동")
    private final String userName;
    
    @Schema(description = "사용자 권한", example = "USER")
    private final String userRole;
    
    @Schema(description = "로그인 시간", example = "2023-12-01T10:30:00")
    private final LocalDateTime loginTime;

    @Schema(description = "응답 메시지", example = "로그인이 성공적으로 완료되었습니다.")
    private final String message;

    @Builder
    private UserLoginResponse(boolean success, String token, String refreshToken, String tokenType, String userName, String userRole, LocalDateTime loginTime, String message) {
        this.success = success;
        this.token = token;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.userName = userName;
        this.userRole = userRole;
        this.loginTime = loginTime;
        this.message = message;
    }

    // 정적 팩토리 메서드 : Refresh Token을 포함한 완전한 로그인 응답 생성
    public static UserLoginResponse withRefreshToken(User user, String accessToken, String refreshToken) {
        return UserLoginResponse.builder()
                .success(true)
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userName(user.getUserName())
                .userRole(user.getUserRole().name())
                .loginTime(LocalDateTime.now())
                .message("로그인이 성공적으로 완료되었습니다.")
                .build();
    }

    // 정적 팩토리 메서드 : 에러 응답 생성 (로그인 실패 시)
    public static UserLoginResponse error(String errorMessage) {
        return UserLoginResponse.builder()
                .success(false)
                .token(null)
                .refreshToken(null)
                .tokenType(null)
                .userName(null)
                .userRole(null)
                .loginTime(null)
                .message(errorMessage)
                .build();
    }
}
