package com.jober.final2teamdrhong.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 이메일 인증 코드 엔티티 (RDB 저장용)
 * Redis나 InMemory가 사용 불가능할 때 fallback으로 사용
 */
@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@SQLRestriction("is_deleted = false")
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Integer verificationId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode;

    @Builder.Default // 1. 빌더를 위한 기본값 설정을 추가
    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP")
    // 2. 필드에 직접 초기화 로직 할당
    private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_VALIDITY_MINUTES);

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Builder.Default
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    // 정적 팩토리 메서드
    public static EmailVerification create(String email, String verificationCode, int validMinutes) {
        return EmailVerification.builder()
                .email(email)
                .verificationCode(verificationCode)
                .expiresAt(LocalDateTime.now().plusMinutes(validMinutes))
                .build();
    }

    // 기본 유효시간으로 생성하는 팩토리 메서드
    public static EmailVerification create(String email, String verificationCode) {
        return create(email, verificationCode, DEFAULT_VALIDITY_MINUTES);
    }

    // 비즈니스 메서드: 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // 비즈니스 메서드: 시도 횟수 증가
    public void incrementAttempts() {
        this.attempts++;
    }

    // 비즈니스 메서드: 사용 처리
    public void markAsUsed() {
        this.isUsed = true;
    }

    // 상수 정의
    private static final int MAX_ATTEMPTS = 5;
    private static final int DEFAULT_VALIDITY_MINUTES = 5;

    // 비즈니스 메서드: 최대 시도 횟수 초과 여부
    public boolean isMaxAttemptsExceeded() {
        return attempts >= MAX_ATTEMPTS;
    }

    // 비즈니스 메서드: 코드 검증
    public boolean verifyCode(String inputCode) {
        return this.verificationCode.equals(inputCode);
    }

    // 비즈니스 메서드: 유효성 검증 (종합)
    public boolean isValid() {
        return !isExpired() && !isUsed && !isMaxAttemptsExceeded();
    }
}
