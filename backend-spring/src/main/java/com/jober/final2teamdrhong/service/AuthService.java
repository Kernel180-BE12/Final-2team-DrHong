package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.dto.auth.SocialSignupRequest;
import com.jober.final2teamdrhong.dto.auth.SocialSignupResponse;
import com.jober.final2teamdrhong.dto.userLogin.TokenRefreshResponse;
import com.jober.final2teamdrhong.dto.userLogin.UserLoginRequest;
import com.jober.final2teamdrhong.dto.userLogin.UserLoginResponse;
import com.jober.final2teamdrhong.dto.userSignup.UserSignupRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.dto.auth.OAuth2UserInfoFactory;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.exception.BusinessException;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import com.jober.final2teamdrhong.util.LogMaskingUtil;
import com.jober.final2teamdrhong.util.TimingAttackProtection;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final VerificationStorage verificationStorage;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistService blacklistService;
    private final AuthProperties authProperties;
    private final TimingAttackProtection timingAttackProtection;
    private final OAuth2TempStorageService oAuth2TempStorageService;
    private final UserValidationService userValidationService;

    /**
     * Rate limiting과 함께 회원가입 처리
     */
    public void signupWithRateLimit(UserSignupRequest requestDto, String clientIp) {
        // Rate limiting 체크
        rateLimitService.checkSignupRateLimit(clientIp, requestDto.email());

        // 기존 회원가입 로직 호출
        signup(requestDto);
    }

    public void signup(UserSignupRequest requestDto) {
        log.info("회원가입 시작: email={}", requestDto.email());

        // 1. 비즈니스 규칙 검증 (기본 유효성 검증은 @Valid에서 처리됨)
        userValidationService.validateLocalSignupBusinessRules(requestDto);

        // 2. 인증 코드 검증
        validateVerificationCode(requestDto.email(), requestDto.verificationCode());

        try {
            // 4. 새로운 User 생성 (로컬 회원가입용)
            User newUser = User.createForLocalSignup(
                    requestDto.userName(),
                    requestDto.email(),
                    requestDto.userNumber()
            );

            // 5. 비밀번호 암호화 및 UserAuth 생성
            String encodedPassword = passwordEncoder.encode(requestDto.password());
            UserAuth userAuth = UserAuth.builder()
                    .authType(UserAuth.AuthType.LOCAL)
                    .passwordHash(encodedPassword)
                    .build();

            // 6. 관계 설정 및 인증 상태 업데이트
            newUser.addUserAuth(userAuth);
            userAuth.markAsVerified(); // 이메일 인증을 완료했으므로 인증 완료 처리
            userRepository.save(newUser);

            log.info("회원가입 성공: userId={}, email={}", newUser.getUserId(), requestDto.email());

            // 주의: 인증 코드는 이미 validateVerificationCode에서 일회성 검증으로 삭제됨

        } catch (Exception e) {
            log.error("회원가입 실패: email={}, error={}", requestDto.email(), e.getMessage());
            throw e;
        }
    }


    private void validateVerificationCode(String email, String inputCode) {
        // Rate limiting 검사: 이메일별 검증 실패 제한
        rateLimitService.checkEmailVerifyRateLimit(email);

        // 일회성 검증: 검증 성공 시 즉시 삭제
        boolean isValid = verificationStorage.validateAndDelete(email, inputCode);

        if (!isValid) {
            log.warn("인증 코드 불일치 또는 만료: email={}", LogMaskingUtil.maskEmail(email));
            throw new AuthenticationException("인증 코드가 일치하지 않거나 만료되었습니다.");
        }

        log.info("인증 코드 검증 성공 (일회성): email={}", LogMaskingUtil.maskEmail(email));
    }

    /**
     * 소셜 로그인 회원가입 완료 처리
     * OAuth2 임시 정보와 핸드폰 번호를 결합하여 회원가입을 완료합니다.
     *
     * @param request 소셜 회원가입 요청 정보
     * @return 회원가입 완료 응답 (JWT 토큰 포함)
     */
    public SocialSignupResponse completeSocialSignup(SocialSignupRequest request) {
        log.info("소셜 회원가입 완료 시작 - 임시 키: {}", request.tempKey());

        try {
            // 1. OAuth2 임시 정보 조회
            OAuth2TempUserInfo tempUserInfo = oAuth2TempStorageService.retrieveTempUserInfo(request.tempKey());
            if (tempUserInfo == null) {
                throw new BusinessException("임시 사용자 정보를 찾을 수 없습니다. 소셜 로그인을 다시 시도해 주세요.");
            }

            // 2. 임시 정보 유효성 재검증
            if (!tempUserInfo.isValid() || !tempUserInfo.isEmailValid()) {
                log.error("임시 정보 유효성 검증 실패 - 키: {}", request.tempKey());
                throw new BusinessException("임시 사용자 정보가 손상되었습니다. 소셜 로그인을 다시 시도해 주세요.");
            }

            // 3. 핸드폰 번호 형식 검증만 수행 (인증 코드 검증 생략)

            // 4. 소셜 회원가입 비즈니스 규칙 검증
            userValidationService.validateSocialSignupBusinessRules(tempUserInfo.email(), tempUserInfo.name(), request);

            // 5. 새로운 User 및 소셜 인증 생성
            User newUser = createSocialUser(tempUserInfo, request);

            // 6. JWT 토큰 발급
            String accessToken = jwtConfig.generateAccessToken(newUser.getUserEmail(), newUser.getUserId());
            String refreshToken = refreshTokenService.createRefreshToken(newUser, "social-signup");

            // 7. 임시 정보 삭제 (회원가입 완료)
            oAuth2TempStorageService.deleteTempUserInfo(request.tempKey());

            log.info("소셜 회원가입 완료 성공 - 사용자 ID: {}, 이메일: {}, 제공자: {}",
                    newUser.getUserId(), tempUserInfo.email(), tempUserInfo.provider());

            // 8. 응답 생성
            return SocialSignupResponse.success(
                    newUser.getUserId(),
                    accessToken,
                    refreshToken,
                    newUser.getUserEmail(),
                    newUser.getUserName(),
                    newUser.getUserRole().name(),
                    tempUserInfo.provider()
            );

        } catch (BusinessException | AuthenticationException e) {
            log.warn("소셜 회원가입 완료 실패 - 임시 키: {}, 오류: {}", request.tempKey(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("소셜 회원가입 완료 중 예상치 못한 오류 - 임시 키: {}, 오류: {}",
                    request.tempKey(), e.getMessage(), e);
            throw new BusinessException("회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }


    /**
     * 소셜 사용자 생성 및 저장
     */
    private User createSocialUser(OAuth2TempUserInfo tempUserInfo, SocialSignupRequest request) {
        try {
            // 1. 새로운 User 생성 (소셜 회원가입용)
            User newUser = User.createForSocialSignup(
                    tempUserInfo.name(),
                    tempUserInfo.email(),
                    request.getNormalizedPhoneNumber()
            );

            // 2. OAuth2 제공자에 따른 AuthType 결정
            UserAuth.AuthType authType = OAuth2UserInfoFactory.getAuthType(tempUserInfo.provider());

            // 3. 소셜 인증 정보 생성
            UserAuth socialAuth = UserAuth.createSocialAuth(
                    newUser,
                    authType,
                    tempUserInfo.socialId()
            );

            // 4. 소셜 인증은 이미 완료된 상태로 설정
            socialAuth.markAsVerified();

            // 5. 관계 설정
            newUser.addUserAuth(socialAuth);

            // 6. 사용자 저장
            userRepository.save(newUser);

            return newUser;

        } catch (Exception e) {
            log.error("소셜 사용자 생성 실패 - 이메일: {}, 제공자: {}, 오류: {}",
                    tempUserInfo.email(), tempUserInfo.provider(), e.getMessage());
            throw new BusinessException("사용자 계정 생성에 실패했습니다.");
        }
    }

    /**
     * 로컬 계정 로그인 (Refresh Token 포함)
     */
    public UserLoginResponse loginWithRefreshToken(@Valid UserLoginRequest userLoginRequest, String clientIp) {
        // 타이밍 공격 방지를 위한 요청 시작 시간 기록
        timingAttackProtection.startTiming();

        try {
            log.info("로그인 시도: email={}", LogMaskingUtil.maskEmail(userLoginRequest.email()));

            AuthenticationResult authResult = authenticateUser(userLoginRequest);
            if (authResult.isFailure()) {
                handleAuthenticationFailure(userLoginRequest.email());
            }
            return createSuccessfulLoginResponse(authResult.user(), authResult.localAuth(), clientIp);

        } catch (BadCredentialsException e) {
            handleAuthenticationFailure(userLoginRequest.email());
            throw e;
        } catch (Exception e) {
            handleUnexpectedError(userLoginRequest.email(), e);
            return null; // 실제로는 예외가 던져지므로 도달하지 않음
        } finally {
            // ThreadLocal 정리 (메모리 누수 방지)
            timingAttackProtection.clear();
        }
    }

    /**
     * 사용자 인증 수행
     */
    private AuthenticationResult authenticateUser(UserLoginRequest request) {
        String targetHash = authProperties.getSecurity().getDummyHash();
        User user = userRepository.findByUserEmailWithAuth(request.email()).orElse(null);
        UserAuth localAuth = null;

        if (user != null) {
            localAuth = user.getUserAuths().stream()
                    .filter(auth -> auth.getAuthType() == UserAuth.AuthType.LOCAL)
                    .findFirst()
                    .orElse(null);

            if (localAuth != null) {
                targetHash = localAuth.getPasswordHash();
            }
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), targetHash);
        return new AuthenticationResult(user, localAuth, passwordMatches);
    }

    /**
     * 인증 실패 처리
     */
    private void handleAuthenticationFailure(String email) {
        timingAttackProtection.ensureMinimumResponseTime(authProperties.getSecurity().getMinResponseTimeMs());
        log.warn("로그인 실패: email={}, reason=인증 정보 불일치", LogMaskingUtil.maskEmail(email));
        throw new BadCredentialsException(authProperties.getMessages().getInvalidCredentials());
    }

    /**
     * 예상치 못한 오류 처리
     */
    private void handleUnexpectedError(String email, Exception e) {
        timingAttackProtection.ensureMinimumResponseTime(authProperties.getSecurity().getMinResponseTimeMs());
        log.error("로그인 처리 중 오류 발생: email={}, error={}",
                LogMaskingUtil.maskEmail(email), e.getMessage());
        throw new BadCredentialsException(authProperties.getMessages().getInvalidCredentials());
    }

    /**
     * 성공적인 로그인 응답 생성
     */
    private UserLoginResponse createSuccessfulLoginResponse(User user, UserAuth localAuth, String clientIp) {
        localAuth.updateLastUsed();
        userRepository.save(user);

        String accessToken = jwtConfig.generateAccessToken(user.getUserEmail(), user.getUserId());
        String refreshToken = refreshTokenService.createRefreshToken(user, clientIp);

        log.info("로그인 성공: userId={}, email={}, role={}",
                LogMaskingUtil.maskUserId(user.getUserId().longValue()),
                LogMaskingUtil.maskEmail(user.getUserEmail()),
                user.getUserRole().name());

        return UserLoginResponse.withRefreshToken(user, accessToken, refreshToken);
    }

    /**
     * 토큰 갱신 처리 (Authorization 헤더에서 Refresh Token 추출)
     */
    public TokenRefreshResponse refreshTokens(String authorizationHeader, String clientIp) {
        // 1. Authorization 헤더에서 Refresh Token 추출
        String refreshToken = jwtConfig.extractTokenFromHeader(authorizationHeader);

        if (refreshToken == null) {
            log.warn("토큰 갱신 실패 - Authorization 헤더 없음: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            throw new AuthenticationException("Authorization 헤더가 필요합니다.");
        }

        // 2. 토큰 갱신 처리
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.refreshTokens(refreshToken, clientIp);

        // 3. 보안 강화: 민감한 정보 마스킹 후 로깅
        log.info("토큰 갱신 API 완료: ip={}", LogMaskingUtil.maskIpAddress(clientIp));

        // 4. 새로운 토큰 쌍 응답
        return TokenRefreshResponse.of(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            jwtConfig.getAccessTokenValiditySeconds()
        );
    }

    /**
     * 로그아웃 처리 (Access Token과 Refresh Token 무효화)
     */
    public void logout(String accessToken, String refreshToken, String clientIp) {
        try {
            // 1. Access Token 블랙리스트 추가
            if (accessToken != null && jwtConfig.validateToken(accessToken) && jwtConfig.isAccessToken(accessToken)) {
                blacklistService.addAccessTokenToBlacklist(accessToken);
                log.info("Access Token이 블랙리스트에 추가되었습니다: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            }

            // 2. Refresh Token 무효화
            if (refreshToken != null && jwtConfig.validateToken(refreshToken) && jwtConfig.isRefreshToken(refreshToken)) {
                refreshTokenService.revokeRefreshToken(refreshToken);
                log.info("Refresh Token이 무효화되었습니다: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            } else if (refreshToken != null) {
                log.warn("유효하지 않은 Refresh Token 형식: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            }

            log.info("로그아웃 완료: ip={}", LogMaskingUtil.maskIpAddress(clientIp));

        } catch (Exception e) {
            log.warn("로그아웃 처리 중 일부 오류 발생: ip={}, error={}",
                     LogMaskingUtil.maskIpAddress(clientIp), e.getMessage());
            // 로그아웃은 부분적 실패라도 성공으로 처리 (클라이언트에서 토큰 삭제가 중요)
        }
    }

    /**
     * 인증 결과를 담는 내부 record 클래스
     */
    private record AuthenticationResult(User user, UserAuth localAuth, boolean passwordMatches) {

        public boolean isFailure() {
            return user == null || localAuth == null || !passwordMatches;
        }
    }
}