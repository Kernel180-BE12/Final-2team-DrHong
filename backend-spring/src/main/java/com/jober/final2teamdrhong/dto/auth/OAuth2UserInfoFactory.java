package com.jober.final2teamdrhong.dto.auth;

import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.exception.BusinessException;

import java.util.Map;

/**
 * OAuth2 제공자별 사용자 정보 객체 생성을 담당하는 팩토리 클래스
 */
public class OAuth2UserInfoFactory {

    /**
     * OAuth2 제공자에 따라 적절한 OAuth2UserInfo 구현체를 반환
     *
     * @param registrationId OAuth2 제공자 ID (google, naver, kakao)
     * @param attributes 사용자 속성 정보
     * @return OAuth2UserInfo 구현체
     * @throws BusinessException 지원하지 않는 제공자인 경우
     */
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        UserAuth.AuthType authType = getAuthType(registrationId);

        switch (authType) {
            case GOOGLE:
                return new GoogleOAuth2UserInfo(attributes);
            case NAVER:
                // TODO: 향후 네이버 로그인 구현 시 추가
                throw new BusinessException("네이버 로그인은 아직 지원하지 않습니다.");
            case KAKAO:
                // TODO: 향후 카카오 로그인 구현 시 추가
                throw new BusinessException("카카오 로그인은 아직 지원하지 않습니다.");
            default:
                throw new BusinessException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }
    }

    /**
     * registrationId를 AuthType으로 변환
     *
     * @param registrationId OAuth2 제공자 ID
     * @return AuthType
     * @throws BusinessException 지원하지 않는 제공자인 경우
     */
    public static UserAuth.AuthType getAuthType(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return UserAuth.AuthType.GOOGLE;
            case "naver":
                return UserAuth.AuthType.NAVER;
            case "kakao":
                return UserAuth.AuthType.KAKAO;
            default:
                throw new BusinessException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }
    }
}