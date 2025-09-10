package com.jober.final2teamdrhong.service.storage;

import com.jober.final2teamdrhong.entity.EmailVerification;
import com.jober.final2teamdrhong.repository.EmailVerificationRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Redis 폴백 테스트용 RDB 저장소 구현체입니다.
 * redis-fallback-test 프로파일에서만 활성화됩니다.
 */
@Component
@Primary
@Transactional
@Profile("redis-fallback-test")
public class TestRdbVerificationStorage implements VerificationStorage {
    
    private final EmailVerificationRepository repository;
    
    public TestRdbVerificationStorage(EmailVerificationRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public void save(String key, String value) {
        // 기존 인증 정보가 있다면 삭제 후 새로 저장
        repository.deleteByEmail(key);
        repository.flush(); // 즉시 삭제 반영
        
        EmailVerification verification = EmailVerification.create(key, value);
        repository.save(verification);
    }

    @Override
    public Optional<String> find(String key) {
        return repository.findByEmail(key)
                .filter(EmailVerification::isValid)
                .map(EmailVerification::getVerificationCode);
    }

    @Override
    public void delete(String key) {
        repository.deleteByEmail(key);
    }
}
