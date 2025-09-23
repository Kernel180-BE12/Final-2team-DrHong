package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.auth.SocialSignupRequest;
import com.jober.final2teamdrhong.dto.userSignup.UserSignupRequest;
import com.jober.final2teamdrhong.exception.BusinessException;
import com.jober.final2teamdrhong.exception.DuplicateResourceException;
import com.jober.final2teamdrhong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 검증 관련 공통 로직을 처리하는 서비스
 * 로컬 회원가입과 소셜 회원가입에서 공통으로 사용되는 검증 로직을 통합합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

    private final UserRepository userRepository;

    /**
     * 이메일 중복 검증
     *
     * @param email 검증할 이메일
     * @throws DuplicateResourceException 이미 가입된 이메일인 경우
     */
    public void validateEmailDuplication(String email) {
        if (userRepository.findByUserEmail(email).isPresent()) {
            log.warn("이메일 중복 감지: {}", email);
            throw new DuplicateResourceException("이미 가입된 이메일입니다.");
        }
        log.debug("이메일 중복 검증 통과: {}", email);
    }

    /**
     * 핸드폰 번호 중복 검증 (향후 계정 통합 시 사용)
     *
     * @param phoneNumber 검증할 핸드폰 번호
     * @throws DuplicateResourceException 이미 가입된 핸드폰 번호인 경우
     */
    public void validatePhoneNumberDuplication(String phoneNumber) {
        // TODO: 계정 통합 로직 구현 시 활성화
        // if (userRepository.findByUserNumber(phoneNumber).isPresent()) {
        //     log.warn("핸드폰 번호 중복 감지: {}", phoneNumber);
        //     throw new DuplicateResourceException("이미 가입된 핸드폰 번호입니다.");
        // }
        log.debug("핸드폰 번호 중복 검증 스킵 (미구현): {}", phoneNumber);
    }

    /**
     * 필수 약관 동의 검증
     *
     * @param termsAgreed 이용약관 동의 여부
     * @param privacyAgreed 개인정보처리방침 동의 여부
     * @throws BusinessException 필수 약관에 동의하지 않은 경우
     */
    public void validateRequiredAgreements(boolean termsAgreed, boolean privacyAgreed) {
        if (!termsAgreed || !privacyAgreed) {
            log.warn("필수 약관 동의하지 않음 - 이용약관: {}, 개인정보: {}", termsAgreed, privacyAgreed);
            throw new BusinessException("필수 약관에 동의해야 합니다.");
        }
        log.debug("필수 약관 동의 검증 통과");
    }

    /**
     * 로컬 회원가입 비즈니스 규칙 검증
     *
     * @param request 로컬 회원가입 요청 정보
     */
    public void validateLocalSignupBusinessRules(UserSignupRequest request) {
        log.debug("로컬 회원가입 비즈니스 규칙 검증 시작: {}", request.email());

        // 1. 이메일 중복 확인
        validateEmailDuplication(request.email());

        // 2. 핸드폰 번호 중복 확인 (향후)
        validatePhoneNumberDuplication(request.userNumber());

        // 3. 추가 비즈니스 규칙들
        validateAdditionalBusinessRules(request.email(), request.userName());

        log.debug("로컬 회원가입 비즈니스 규칙 검증 완료: {}", request.email());
    }

    /**
     * 소셜 회원가입 비즈니스 규칙 검증
     *
     * @param email 소셜 계정 이메일
     * @param name 소셜 계정 이름
     * @param request 소셜 회원가입 요청 정보
     */
    public void validateSocialSignupBusinessRules(String email, String name, SocialSignupRequest request) {
        log.debug("소셜 회원가입 비즈니스 규칙 검증 시작: {}", email);

        // 1. 이메일 중복 확인
        validateEmailDuplication(email);

        // 2. 핸드폰 번호 중복 확인 (향후)
        validatePhoneNumberDuplication(request.getNormalizedPhoneNumber());

        // 3. 필수 약관 동의 확인
        validateRequiredAgreements(request.agreedToTerms(), request.agreedToPrivacyPolicy());

        // 4. 추가 비즈니스 규칙들
        validateAdditionalBusinessRules(email, name);

        log.debug("소셜 회원가입 비즈니스 규칙 검증 완료: {}", email);
    }

    /**
     * 추가 비즈니스 규칙 검증
     * 이메일 도메인 제한, 사용자명 금지어 체크 등의 로직을 여기에 구현
     *
     * @param email 이메일
     * @param userName 사용자명
     */
    private void validateAdditionalBusinessRules(String email, String userName) {
        log.debug("추가 비즈니스 규칙 검증 시작: email={}, userName={}", email, userName);

        // TODO: 향후 필요한 비즈니스 규칙들 구현
        // 예시:
        // - 이메일 도메인 허용/차단 목록 확인
        // - 사용자명 금지어 필터링
        // - 사용자명 길이 및 형식 검증
        // - 비속어 필터링

        log.debug("추가 비즈니스 규칙 검증 완료");
    }

    /**
     * 계정 통합 가능성 검사 (향후 구현)
     *
     * @param email 이메일
     * @param phoneNumber 핸드폰 번호
     * @return 통합 가능한 기존 계정이 있는지 여부
     */
    public boolean canMergeWithExistingAccount(String email, String phoneNumber) {
        // TODO: 계정 통합 로직 구현 시 활성화
        // 1. 동일 이메일로 가입된 로컬 계정 확인
        // 2. 동일 핸드폰 번호로 가입된 계정 확인
        // 3. 통합 가능 여부 판단

        log.debug("계정 통합 가능성 검사 스킵 (미구현): email={}, phone={}", email, phoneNumber);
        return false;
    }
}