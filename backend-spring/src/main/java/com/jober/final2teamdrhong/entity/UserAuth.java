package com.jober.final2teamdrhong.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Integer authId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 양방향 관계 설정을 위한 편의 메서드
    protected void setUser(User user) {
        this.user = user;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false)
    private AuthType authType;

    @Column(name = "social_id")
    private String socialId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public enum AuthType {
        LOCAL, GOOGLE, KAKAO, NAVER
    }


    @Builder
    private UserAuth(User user, AuthType authType, String socialId, String passwordHash) {
        this.user = user;
        this.authType = authType;
        this.socialId = socialId;
        this.passwordHash = passwordHash;
        this.isPrimary = true; // 기본값은 주 인증수단
        this.isVerified = (authType != AuthType.LOCAL); // 소셜 인증은 바로 true
        this.linkedAt = LocalDateTime.now();
    }

    // --- 비즈니스 메서드 ---
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void markAsVerified() {
        if (!this.isVerified) {
            this.isVerified = true;
        }
    }

    public void setAsPrimary() {
        this.isPrimary = true;
    }

    public void unsetAsPrimary() {
        this.isPrimary = false;
    }
}