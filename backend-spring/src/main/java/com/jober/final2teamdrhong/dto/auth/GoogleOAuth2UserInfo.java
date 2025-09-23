package com.jober.final2teamdrhong.dto.auth;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보 구현체
 * Google에서 제공하는 사용자 정보를 처리하는 클래스
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        // Google의 고유 사용자 ID (sub 필드)
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        // Google 사용자 이름 (name 필드)
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        // Google 이메일 (email 필드)
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        // Google 프로필 이미지 URL (picture 필드)
        return (String) attributes.get("picture");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    /**
     * 이메일 인증 여부 확인
     * @return 이메일 인증 여부
     */
    public Boolean isEmailVerified() {
        return (Boolean) attributes.get("email_verified");
    }

    /**
     * 주어진 이름 (First Name) 반환
     * @return 주어진 이름
     */
    public String getGivenName() {
        return (String) attributes.get("given_name");
    }

    /**
     * 성 (Family Name) 반환
     * @return 성
     */
    public String getFamilyName() {
        return (String) attributes.get("family_name");
    }

    /**
     * 로케일 정보 반환
     * @return 사용자 로케일
     */
    public String getLocale() {
        return (String) attributes.get("locale");
    }
}