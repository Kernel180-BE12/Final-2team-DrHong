package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.UserLogin.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserLogin.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignup.UserSignupRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.exception.DuplicateResourceException;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import com.jober.final2teamdrhong.util.LogMaskingUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class UserService {

    private final VerificationStorage verificationStorage;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;

    public UserService(VerificationStorage verificationStorage, UserRepository userRepository, PasswordEncoder passwordEncoder, RateLimitService rateLimitService, JwtConfig jwtConfig, RefreshTokenService refreshTokenService, AuthProperties authProperties) {
        this.verificationStorage = verificationStorage;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimitService = rateLimitService;
        this.jwtConfig = jwtConfig;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
    }

    /**
     * Rate limiting과 함께 회원가입 처리
     */
    public void signupWithRateLimit(UserSignupRequest requestDto, String clientIp) {
        // Rate limiting 체크
        rateLimitService.checkSignupRateLimit(clientIp, requestDto.getEmail());
        
        // 기존 회원가입 로직 호출
        signup(requestDto);
    }

    public void signup(UserSignupRequest requestDto) {
        log.info("회원가입 시작: email={}", requestDto.getEmail());
        
        // 1. 비즈니스 규칙 검증 (기본 유효성 검증은 @Valid에서 처리됨)
        validateBusinessRules(requestDto);
        
        // 2. 인증 코드 검증
        validateVerificationCode(requestDto.getEmail(), requestDto.getVerificationCode());

        try {
            // 4. 새로운 User 생성
            User newUser = User.create(
                    requestDto.getUserName(),
                    requestDto.getEmail(),
                    requestDto.getUserNumber()
            );

            // 5. 비밀번호 암호화 및 UserAuth 생성
            String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
            UserAuth userAuth = UserAuth.builder()
                    .authType(UserAuth.AuthType.LOCAL)
                    .passwordHash(encodedPassword)
                    .build();

            // 6. 관계 설정 및 인증 상태 업데이트
            newUser.addUserAuth(userAuth);
            userAuth.markAsVerified(); // 이메일 인증을 완료했으므로 인증 완료 처리
            userRepository.save(newUser);
            
            log.info("회원가입 성공: userId={}, email={}", newUser.getUserId(), requestDto.getEmail());
            
            // 주의: 인증 코드는 이미 validateVerificationCode에서 일회성 검증으로 삭제됨
            
        } catch (Exception e) {
            log.error("회원가입 실패: email={}, error={}", requestDto.getEmail(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * 회원가입 성공 후 인증 코드 삭제
     * 별도 트랜잭션으로 실행하여 회원가입 실패 시에도 인증 코드가 유지되도록 함
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void deleteVerificationCodeAfterSuccess(String email) {
        try {
            verificationStorage.delete(email);
            log.info("인증 코드 삭제 완료: email={}", email);
        } catch (Exception e) {
            // 인증 코드 삭제 실패는 회원가입에 영향주지 않음
            log.warn("인증 코드 삭제 실패 (회원가입은 성공): email={}, error={}", email, e.getMessage());
        }
    }
    
    private void validateBusinessRules(UserSignupRequest requestDto) {
        // 이메일 중복 확인 (비즈니스 규칙)
        if (userRepository.findByUserEmail(requestDto.getEmail()).isPresent()) {
            throw new DuplicateResourceException("이미 가입된 이메일입니다.");
        }
        
        // 추가 비즈니스 규칙들이 여기에 들어갈 수 있음
        // 예: 이메일 도메인 제한, 사용자명 금지어 체크 등
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
     * 타이밍 공격을 방지하기 위한 상수 시간 문자열 비교
     */
    private boolean constantTimeEquals(String a, String b) {
        // null 값은 항상 false (보안상 null == null도 인증 실패)
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * 타이밍 공격 방지를 위한 최소 응답 시간 보장
     * @param minimumMs 최소 대기 시간 (밀리초)
     */
    private void ensureMinimumResponseTime(long minimumMs) {
        try {
            long currentTime = System.currentTimeMillis();
            // 요청 시작 시간을 현재 스레드에 저장된 값에서 가져오거나, 현재 시간 사용
            long startTime = getCurrentRequestStartTime();
            long elapsed = currentTime - startTime;
            
            if (elapsed < minimumMs) {
                long sleepTime = minimumMs - elapsed;
                log.debug("보안 지연 적용: {}ms 대기", sleepTime);
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            // 인터럽트 상태 복원
            Thread.currentThread().interrupt();
            log.warn("응답 시간 지연 중 인터럽트 발생");
        }
    }
    
    /**
     * 현재 요청의 시작 시간을 반환 (없으면 현재 시간 - 100ms)
     */
    private long getCurrentRequestStartTime() {
        // 실제로는 요청 시작 시간을 ThreadLocal에 저장하거나 
        // Spring의 RequestAttributes를 사용할 수 있지만,
        // 단순화를 위해 현재 시간에서 100ms를 뺀 값을 사용
        return System.currentTimeMillis() - 100;
    }

    /**
     * 로컬 계정 로그인 (Refresh Token 포함)
     */
    public UserLoginResponse loginWithRefreshToken(@Valid UserLoginRequest userLoginRequest, String clientIp) {
        log.info("로그인 시도: email={}", LogMaskingUtil.maskEmail(userLoginRequest.getEmail()));
        
        try {
            AuthenticationResult authResult = authenticateUser(userLoginRequest);
            if (authResult.isFailure()) {
                handleAuthenticationFailure(userLoginRequest.getEmail());
            }
            return createSuccessfulLoginResponse(authResult.getUser(), authResult.getLocalAuth(), clientIp);
            
        } catch (BadCredentialsException e) {
            handleAuthenticationFailure(userLoginRequest.getEmail());
            throw e;
        } catch (Exception e) {
            handleUnexpectedError(userLoginRequest.getEmail(), e);
            return null; // 실제로는 예외가 던져지므로 도달하지 않음
        }
    }
    
    /**
     * 사용자 인증 수행
     */
    private AuthenticationResult authenticateUser(UserLoginRequest request) {
        String targetHash = authProperties.getSecurity().getDummyHash();
        User user = userRepository.findByUserEmailWithAuth(request.getEmail()).orElse(null);
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
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), targetHash);
        return new AuthenticationResult(user, localAuth, passwordMatches);
    }
    
    /**
     * 인증 실패 처리
     */
    private void handleAuthenticationFailure(String email) {
        ensureMinimumResponseTime(authProperties.getSecurity().getMinResponseTimeMs());
        log.warn("로그인 실패: email={}, reason=인증 정보 불일치", LogMaskingUtil.maskEmail(email));
        throw new BadCredentialsException(authProperties.getMessages().getInvalidCredentials());
    }
    
    /**
     * 예상치 못한 오류 처리
     */
    private void handleUnexpectedError(String email, Exception e) {
        ensureMinimumResponseTime(authProperties.getSecurity().getMinResponseTimeMs());
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
     * 인증 결과를 담는 내부 클래스
     */
    private static class AuthenticationResult {
        private final User user;
        private final UserAuth localAuth;
        private final boolean passwordMatches;
        
        public AuthenticationResult(User user, UserAuth localAuth, boolean passwordMatches) {
            this.user = user;
            this.localAuth = localAuth;
            this.passwordMatches = passwordMatches;
        }
        
        public boolean isFailure() {
            return user == null || localAuth == null || !passwordMatches;
        }
        
        public User getUser() { return user; }
        public UserAuth getLocalAuth() { return localAuth; }
    }
    
}