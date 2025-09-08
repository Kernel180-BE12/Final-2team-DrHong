package com.jober.final2teamdrhong.dto;

import com.jober.final2teamdrhong.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserLoginResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final String userName;
    private final String userRole;

    @Builder
    private UserLoginResponse(String token, String userName, String userRole) {
        this.token = token;
        this.userName = userName;
        this.userRole = userRole;
    }

    // 정적 팩토리 메서드 : user 객체와 토큰을 받아 Dto 생성
    public static UserLoginResponse of(User user, String token) {
        return UserLoginResponse.builder()
                .token(token)
                .userName(user.getUserName())
                .userRole(user.getUserRole().name())
                .build();
    }
}
