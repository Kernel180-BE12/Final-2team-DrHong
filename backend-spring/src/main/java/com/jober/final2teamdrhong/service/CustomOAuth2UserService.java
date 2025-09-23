package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.auth.OAuth2UserInfo;
import com.jober.final2teamdrhong.dto.auth.OAuth2UserInfoFactory;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.exception.BusinessException;
import com.jober.final2teamdrhong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;

/**
 * OAuth2 사용자 정보를 처리하는 커스텀 서비스
 * Google 등의 OAuth2 제공자로부터 받은 사용자 정보를 처리하고,
 * 기존 사용자인지 신규 사용자인지 판단하여 적절한 처리를 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            // 기본 OAuth2 사용자 정보 로드
            OAuth2User oauth2User = super.loadUser(userRequest);

            // OAuth2 제공자 정보 추출
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            log.info("OAuth2 로그인 시도 - 제공자: {}, 사용자 속성명: {}", registrationId, userNameAttributeName);

            // OAuth2 사용자 정보 파싱
            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                    registrationId,
                    oauth2User.getAttributes()
            );

            // 필수 정보 검증
            validateOAuth2UserInfo(oAuth2UserInfo);

            // 기존 사용자 확인
            Optional<User> existingUser = userRepository.findByUserEmail(oAuth2UserInfo.getEmail());

            if (existingUser.isPresent()) {
                // 기존 사용자 처리
                User user = existingUser.get();
                log.info("기존 사용자 OAuth2 로그인: {}", user.getUserEmail());

                // 해당 제공자로 이미 연결된 계정이 있는지 확인
                UserAuth.AuthType authType = OAuth2UserInfoFactory.getAuthType(registrationId);
                boolean hasExistingAuth = user.getUserAuths().stream()
                        .anyMatch(auth -> auth.getAuthType() == authType &&
                                 auth.getSocialId().equals(oAuth2UserInfo.getId()));

                if (!hasExistingAuth) {
                    // 새로운 소셜 로그인 방식 추가
                    UserAuth newAuth = UserAuth.createSocialAuth(user, authType, oAuth2UserInfo.getId());
                    user.addUserAuth(newAuth);
                    log.info("사용자 {}에 새로운 OAuth2 인증 방식 추가: {}", user.getUserEmail(), authType);
                }

                // Spring Security가 사용할 OAuth2User 객체 생성 (기존 사용자)
                return createOAuth2User(oAuth2UserInfo, userNameAttributeName, user, true);
            } else {
                // 신규 사용자 처리 - 임시 정보로 OAuth2User 생성
                log.info("신규 사용자 OAuth2 로그인: {}", oAuth2UserInfo.getEmail());
                return createOAuth2User(oAuth2UserInfo, userNameAttributeName, null, false);
            }

        } catch (Exception e) {
            log.error("OAuth2 사용자 정보 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("OAuth2 사용자 정보 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * OAuth2 사용자 정보 필수 항목 검증
     */
    private void validateOAuth2UserInfo(OAuth2UserInfo oAuth2UserInfo) {
        if (!StringUtils.hasText(oAuth2UserInfo.getId())) {
            throw new BusinessException("OAuth2 사용자 ID가 없습니다.");
        }
        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new BusinessException("OAuth2 사용자 이메일이 없습니다.");
        }
        if (!StringUtils.hasText(oAuth2UserInfo.getName())) {
            throw new BusinessException("OAuth2 사용자 이름이 없습니다.");
        }
    }

    /**
     * Spring Security가 사용할 OAuth2User 객체 생성
     */
    private OAuth2User createOAuth2User(OAuth2UserInfo oAuth2UserInfo, String userNameAttributeName,
                                       User user, boolean isExistingUser) {
        // 권한 설정 (기존 사용자는 실제 권한, 신규 사용자는 임시 권한)
        String authority = isExistingUser ?
                "ROLE_" + user.getUserRole().name() :
                "ROLE_OAUTH2_TEMPORARY";

        // 사용자 속성에 추가 정보 포함
        var attributes = new java.util.HashMap<>(oAuth2UserInfo.getAttributes());
        attributes.put("isExistingUser", isExistingUser);
        attributes.put("provider", oAuth2UserInfo.getProvider());
        if (isExistingUser) {
            attributes.put("userId", user.getUserId());
            attributes.put("userRole", user.getUserRole().name());
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(authority)),
                attributes,
                userNameAttributeName
        );
    }
}