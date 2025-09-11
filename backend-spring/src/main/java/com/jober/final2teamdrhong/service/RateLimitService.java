package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.config.RateLimitConfig;
import com.jober.final2teamdrhong.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Rate Limiting 서비스
 * 
 * 기능별 Rate Limiting 제공:
 * - 이메일 발송: IP당 5분간 3회
 * - 이메일 인증: 이메일당 10분간 5회  
 * - 회원가입: IP당 1시간간 10회
 * - 로그인: IP당 15분간 5회 + 이메일당 15분간 3회 (이중 체크)
 * - 토큰 갱신: IP당 5분간 10회
 * 
 * Redis 사용 가능시 분산 환경 지원, 불가능시 인메모리 폴백 사용
 */
@Service
@Slf4j
public class RateLimitService {
    
    private final RateLimitConfig rateLimitConfig;
    private final LettuceBasedProxyManager<byte[]> proxyManager;
    
    // 인메모리 버킷 저장소 (Redis 비활성화 시 폴백용)
    private final Map<String, Bucket> inMemoryBuckets = new ConcurrentHashMap<>();
    
    // 버킷 생성 동기화용 락 (동일 키에 대한 중복 생성 방지)
    private final Map<String, ReentrantLock> bucketLocks = new ConcurrentHashMap<>();
    
    @Autowired
    public RateLimitService(RateLimitConfig rateLimitConfig, 
                           @Autowired(required = false) StringRedisTemplate redisTemplate,
                           @Autowired(required = false) LettuceBasedProxyManager<byte[]> proxyManager) {
        this.rateLimitConfig = rateLimitConfig;
        this.proxyManager = proxyManager;
    }
    
    // =========================================
    // 허용 여부 체크 메서드들 (Allowed Methods)
    // =========================================
    
    public boolean isEmailSendAllowed(String ipAddress) {
        String key = "email_send:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createEmailSendBucketConfig());
        return bucket.tryConsume(1);
    }
    
    public boolean isEmailVerifyAllowed(String email) {
        String key = "email_verify:" + email;
        Bucket bucket = getBucket(key, () -> createEmailVerifyBucketConfig());
        return bucket.tryConsume(1);
    }
    
    public boolean isSignupAllowed(String ipAddress) {
        String key = "signup:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createSignupBucketConfig());
        return bucket.tryConsume(1);
    }
    
    public boolean isLoginAllowed(String ipAddress) {
        String key = "login:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createLoginBucketConfig());
        return bucket.tryConsume(1);
    }
    
    /**
     * 이메일별 로그인 시도 허용 여부 체크
     */
    public boolean isLoginByEmailAllowed(String email) {
        String key = "login_email:" + email;
        Bucket bucket = getBucket(key, () -> createLoginByEmailBucketConfig());
        return bucket.tryConsume(1);
    }
    
    /**
     * 토큰 갱신 허용 여부 체크
     */
    public boolean isRefreshTokenAllowed(String ipAddress) {
        String key = "refresh_token:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createRefreshTokenBucketConfig());
        return bucket.tryConsume(1);
    }
    
    // =========================================
    // 대기 시간 계산 메서드들 (Wait Time Methods)  
    // =========================================
    
    public long getEmailSendWaitTime(String ipAddress) {
        String key = "email_send:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createEmailSendBucketConfig());
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    
    public long getEmailVerifyWaitTime(String email) {
        String key = "email_verify:" + email;
        Bucket bucket = getBucket(key, () -> createEmailVerifyBucketConfig());
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    
    public long getSignupWaitTime(String ipAddress) {
        String key = "signup:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createSignupBucketConfig());
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    
    public long getLoginWaitTime(String ipAddress) {
        String key = "login:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createLoginBucketConfig());
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    
    /**
     * 이메일별 로그인 대기 시간 계산
     */
    public long getLoginByEmailWaitTime(String email) {
        String key = "login_email:" + email;
        Bucket bucket = getBucket(key, () -> createLoginByEmailBucketConfig());
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    
    /**
     * 토큰 갱신 대기 시간 계산
     */
    public long getRefreshTokenWaitTime(String ipAddress) {
        String key = "refresh_token:" + ipAddress;
        Bucket bucket = getBucket(key, () -> createRefreshTokenBucketConfig());
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    
    // =========================================
    // 버킷 설정 메서드들 (Bucket Config Methods)
    // =========================================
    
    private BucketConfiguration createEmailSendBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        rateLimitConfig.getEmailSend().getRequestsPerWindow(),
                        Duration.ofMinutes(rateLimitConfig.getEmailSend().getWindowDurationMinutes())
                ))
                .build();
    }
    
    private BucketConfiguration createEmailVerifyBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        rateLimitConfig.getEmailVerify().getRequestsPerWindow(),
                        Duration.ofMinutes(rateLimitConfig.getEmailVerify().getWindowDurationMinutes())
                ))
                .build();
    }
    
    private BucketConfiguration createSignupBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        rateLimitConfig.getSignup().getRequestsPerWindow(),
                        Duration.ofMinutes(rateLimitConfig.getSignup().getWindowDurationMinutes())
                ))
                .build();
    }

    private BucketConfiguration createLoginBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        rateLimitConfig.getLogin().getRequestsPerWindow(),
                        Duration.ofMinutes(rateLimitConfig.getLogin().getWindowDurationMinutes())
                ))
                .build();
    }
    
    /**
     * 이메일별 로그인 버킷 설정 (더 엄격한 제한)
     * 동일 계정에 대한 무차별 대입 공격 방지
     */
    private BucketConfiguration createLoginByEmailBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        3, // 이메일별로는 더 엄격하게 3회로 제한
                        Duration.ofMinutes(rateLimitConfig.getLogin().getWindowDurationMinutes())
                ))
                .build();
    }
    
    /**
     * 토큰 갱신 버킷 설정
     * 토큰 갱신 남용 방지
     */
    private BucketConfiguration createRefreshTokenBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        rateLimitConfig.getRefreshToken().getRequestsPerWindow(),
                        Duration.ofMinutes(rateLimitConfig.getRefreshToken().getWindowDurationMinutes())
                ))
                .build();
    }
    
    // =========================================
    // 내부 헬퍼 메서드들 (Internal Helpers)
    // =========================================
    
    private Bucket getBucket(String key, Supplier<BucketConfiguration> configSupplier) {
        if (proxyManager != null) {
            // Redis 사용 가능한 경우
            return proxyManager.builder()
                    .build(key.getBytes(), configSupplier);
        } else {
            // Redis 비활성화 시 인메모리 폴백 사용 (동기화된 버킷 생성)
            return getOrCreateInMemoryBucket(key, configSupplier);
        }
    }
    
    /**
     * 인메모리 버킷 동기화된 생성/조회
     * 동일 키에 대한 중복 생성 방지
     */
    private Bucket getOrCreateInMemoryBucket(String key, Supplier<BucketConfiguration> configSupplier) {
        // 이미 존재하는 버킷 반환
        Bucket existingBucket = inMemoryBuckets.get(key);
        if (existingBucket != null) {
            return existingBucket;
        }
        
        // 키별 락 획득 (동일 키에 대한 동시 생성 방지)
        ReentrantLock lock = bucketLocks.computeIfAbsent(key, k -> new ReentrantLock());
        
        lock.lock();
        try {
            // 다시 한 번 확인 (다른 스레드가 이미 생성했을 수 있음)
            existingBucket = inMemoryBuckets.get(key);
            if (existingBucket != null) {
                return existingBucket;
            }
            
            // 새 버킷 생성 및 저장
            Bucket newBucket = Bucket.builder()
                    .addLimit(getBandwidthFromConfig(configSupplier.get()))
                    .build();
            
            inMemoryBuckets.put(key, newBucket);
            return newBucket;
            
        } finally {
            lock.unlock();
        }
    }
    
    private Bandwidth getBandwidthFromConfig(BucketConfiguration config) {
        // BucketConfiguration에서 첫 번째 Bandwidth 추출
        Bandwidth[] bandwidths = config.getBandwidths();
        if (bandwidths.length == 0) {
            throw new IllegalArgumentException("BucketConfiguration must have at least one bandwidth");
        }
        return bandwidths[0];
    }
    
    // =========================================
    // 편의 메서드들 (Public API Methods)
    // =========================================
    
    /**
     * 이메일 발송 Rate Limiting 체크
     */
    public void checkEmailSendRateLimit(String clientIp, String email) {
        if (!isEmailSendAllowed(clientIp)) {
            long waitTime = getEmailSendWaitTime(clientIp);
            log.warn("이메일 발송 속도 제한 초과: ip={}, email={}, waitTime={}초", 
                    clientIp, email, waitTime);
            throw new RateLimitExceededException(
                "이메일 발송 속도 제한을 초과했습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
    }

    /**
     * 회원가입 Rate Limiting 체크
     */
    public void checkSignupRateLimit(String clientIp, String email) {
        if (!isSignupAllowed(clientIp)) {
            long waitTime = getSignupWaitTime(clientIp);
            log.warn("회원가입 속도 제한 초과: ip={}, email={}, waitTime={}초", 
                    clientIp, email, waitTime);
            throw new RateLimitExceededException(
                "회원가입 속도 제한을 초과했습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
    }

    /**
     * 이메일 인증 코드 검증 Rate Limiting 체크
     */
    public void checkEmailVerifyRateLimit(String email) {
        if (!isEmailVerifyAllowed(email)) {
            long waitTime = getEmailVerifyWaitTime(email);
            log.warn("인증 코드 검증 속도 제한 초과: email={}, waitTime={}초", email, waitTime);
            throw new RateLimitExceededException(
                "인증 코드 검증 시도 횟수를 초과했습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
    }

    
    /**
     * 로그인 Rate Limiting 체크 (IP 기반만)
     */
    public void checkLoginRateLimit(String clientIp) {
        if (!isLoginAllowed(clientIp)) {
            long waitTime = getLoginWaitTime(clientIp);
            log.warn("로그인 시도 속도 제한 초과: ip={}, waitTime={}초", clientIp, waitTime);
            throw new RateLimitExceededException(
                "로그인 시도 속도 제한을 초과했습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
    }
    
    /**
     * 향상된 로그인 Rate Limiting 체크 (IP + 이메일 기반)
     * 무차별 대입 공격과 계정별 공격을 모두 방지
     */
    public void checkEnhancedLoginRateLimit(String clientIp, String email) {
        // 1. IP 기반 체크
        if (!isLoginAllowed(clientIp)) {
            long waitTime = getLoginWaitTime(clientIp);
            log.warn("로그인 시도 속도 제한 초과 (IP): ip={}, email={}, waitTime={}초", 
                    clientIp, email, waitTime);
            throw new RateLimitExceededException(
                "로그인 시도 속도 제한을 초과했습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
        
        // 2. 이메일 기반 체크 (더 엄격한 제한)
        if (!isLoginByEmailAllowed(email)) {
            long waitTime = getLoginByEmailWaitTime(email);
            log.warn("로그인 시도 속도 제한 초과 (이메일): ip={}, email={}, waitTime={}초", 
                    clientIp, email, waitTime);
            throw new RateLimitExceededException(
                "해당 계정에 대한 로그인 시도가 너무 많습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
    }

    /**
     * 토큰 갱신 Rate Limiting 체크
     * 토큰 갱신 남용 방지
     */
    public void checkRefreshTokenRateLimit(String clientIp) {
        if (!isRefreshTokenAllowed(clientIp)) {
            long waitTime = getRefreshTokenWaitTime(clientIp);
            log.warn("토큰 갱신 속도 제한 초과: ip={}, waitTime={}초", clientIp, waitTime);
            throw new RateLimitExceededException(
                "토큰 갱신 속도 제한을 초과했습니다. " + waitTime + "초 후 다시 시도해주세요.", 
                waitTime
            );
        }
    }

}