package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignupRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.UserAuth;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.service.storage.VerificationStorage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.jober.final2teamdrhong.exception.AuthenticationException;
import com.jober.final2teamdrhong.exception.DuplicateResourceException;
import com.jober.final2teamdrhong.exception.BusinessException;
import com.jober.final2teamdrhong.util.LogMaskingUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final VerificationStorage verificationStorage;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;

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
            
            // 7. 회원가입 성공 후에만 인증 코드 삭제 (트랜잭션 외부에서 실행)
            deleteVerificationCodeAfterSuccess(requestDto.getEmail());
            
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
        
        String savedCode = verificationStorage.find(email)
                .orElseThrow(() -> new BusinessException("인증 코드가 만료되었거나 유효하지 않습니다."));
        
        if (!constantTimeEquals(savedCode, inputCode)) {
            log.warn("인증 코드 불일치: email={}", LogMaskingUtil.maskEmail(email));
            throw new AuthenticationException("인증 코드가 일치하지 않습니다.");
        }
        
        log.info("인증 코드 검증 성공: email={}", LogMaskingUtil.maskEmail(email));
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
     * 로컬 계정 로그인 (Refresh Token 포함)
     * 
     * @param userLoginRequest 로그인 요청 정보
     * @param clientIp 클라이언트 IP 주소
     * @return JWT 토큰과 사용자 정보를 포함한 로그인 응답
     * @throws BadCredentialsException 인증 실패 시 (이메일/비밀번호 불일치, LOCAL 계정 없음)
     */
    public UserLoginResponse loginWithRefreshToken(@Valid UserLoginRequest userLoginRequest, String clientIp) {
        log.info("로그인 시도: email={}", LogMaskingUtil.maskEmail(userLoginRequest.getEmail()));
        
        // 보안상 모든 인증 실패 상황에서 동일한 에러 메시지 사용
        final String INVALID_CREDENTIALS_MESSAGE = "이메일 또는 비밀번호가 일치하지 않습니다.";
        
        try {
            // 1. 이메일 기반으로 User 조회
            User user = userRepository.findByUserEmail(userLoginRequest.getEmail())
                    .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));
            
            // 2. LOCAL 인증 방식 조회
            UserAuth localAuth = user.getUserAuths().stream()
                    .filter(auth -> auth.getAuthType() == UserAuth.AuthType.LOCAL)
                    .findFirst()
                    .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

            // 3. 비밀번호 검증 (타이밍 공격 방지를 위한 상수시간 비교)
            if (!passwordEncoder.matches(userLoginRequest.getPassword(), localAuth.getPasswordHash())) {
                log.warn("로그인 실패 - 비밀번호 불일치: email={}", LogMaskingUtil.maskEmail(userLoginRequest.getEmail()));
                throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
            }

            // 4. 로그인 성공 처리: 마지막 로그인 시간 업데이트
            localAuth.updateLastUsed();
            userRepository.save(user);

            // 5. Access Token과 Refresh Token 생성
            String accessToken = jwtConfig.generateAccessToken(user.getUserEmail(), user.getUserId().longValue());
            
            // 6. Refresh Token 생성 및 저장
            String refreshToken = refreshTokenService.createRefreshToken(user, clientIp);
            
            // 보안 강화: 민감한 정보 마스킹 후 로그인 성공 로깅
            log.info("로그인 성공: userId={}, email={}, role={}", 
                    LogMaskingUtil.maskUserId(user.getUserId().longValue()), 
                    LogMaskingUtil.maskEmail(user.getUserEmail()),
                    user.getUserRole().name());
            
            return UserLoginResponse.withRefreshToken(user, accessToken, refreshToken);
            
        } catch (BadCredentialsException e) {
            // 인증 실패 시 로깅 (보안을 위해 실패 사유는 노출하지 않음)
            log.warn("로그인 실패: email={}, reason=인증 정보 불일치", 
                    LogMaskingUtil.maskEmail(userLoginRequest.getEmail()));
            throw e;
        } catch (Exception e) {
            // 예상치 못한 오류 시 로깅
            log.error("로그인 처리 중 오류 발생: email={}, error={}", 
                    LogMaskingUtil.maskEmail(userLoginRequest.getEmail()), e.getMessage());
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
    }

}