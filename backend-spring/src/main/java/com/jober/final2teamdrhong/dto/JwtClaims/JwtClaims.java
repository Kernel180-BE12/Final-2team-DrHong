package com.jober.final2teamdrhong.dto.JwtClaims;

import com.jober.final2teamdrhong.entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JWT 토큰의 Claims를 담는 간단한 클래스
 * 워크스페이스에서 사용자 정보 접근 시 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {
    
    /**
     * 사용자 이메일 (JWT subject)
     */
    private String email;
    
    /**
     * 사용자 ID
     */
    private Integer userId;
    
    /**
     * 사용자 이름
     */
    private String userName;
    
    /**
     * 사용자 권한
     */
    private UserRole userRole;
    
    /**
     * 토큰 타입 (access/refresh)
     */
    private String tokenType;
    
    /**
     * JWT ID
     */
    private String jti;
    
    /**
     * 토큰 만료 시간
     */
    private LocalDateTime expiresAt;
    
    // 편의 메서드
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(userRole);
    }
    
    public boolean isAccessToken() {
        return "access".equals(tokenType);
    }
    
    public boolean isRefreshToken() {
        return "refresh".equals(tokenType);
    }
}