package com.jober.final2teamdrhong.dto.auth;

import java.util.Map;

/**
 * OAuth2 사용자 정보를 추상화하는 인터페이스
 * 다양한 OAuth2 제공자(Google, Naver, Kakao)의 사용자 정보를 통일된 방식으로 처리하기 위한 인터페이스
 */
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * OAuth2 제공자의 고유 사용자 ID 반환
     * @return 사용자 고유 ID
     */
    public abstract String getId();

    /**
     * 사용자 이름 반환
     * @return 사용자 이름
     */
    public abstract String getName();

    /**
     * 사용자 이메일 반환
     * @return 사용자 이메일
     */
    public abstract String getEmail();

    /**
     * 사용자 프로필 이미지 URL 반환
     * @return 프로필 이미지 URL (선택사항)
     */
    public abstract String getImageUrl();

    /**
     * OAuth2 제공자 이름 반환 (google, naver, kakao 등)
     * @return 제공자 이름
     */
    public abstract String getProvider();
}