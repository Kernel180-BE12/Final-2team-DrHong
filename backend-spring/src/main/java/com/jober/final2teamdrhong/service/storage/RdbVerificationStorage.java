package com.jober.final2teamdrhong.service.storage;

import com.jober.final2teamdrhong.entity.EmailVerification;
import com.jober.final2teamdrhong.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 관계형 데이터베이스(RDB)를 사용하는 저장소 구현체입니다.
 * 데이터의 영속성을 보장하며, Redis 장애 시 Fallback 역할을 합니다.
 */
@Component("rdbStorage")
@RequiredArgsConstructor
@Transactional
public class RdbVerificationStorage implements VerificationStorage {
    
    private final EmailVerificationRepository repository;
    
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
    
    /**
     * 트랜잭션 기반 일회성 검증 및 삭제
     * DB 레벨에서 동시성 문제 해결
     */
    @Override
    @Transactional
    public boolean validateAndDelete(String key, String expectedValue) {
        Optional<EmailVerification> verification = repository.findByEmail(key);
        
        if (verification.isPresent() && 
            verification.get().isValid() && 
            verification.get().getVerificationCode().equals(expectedValue)) {
            
            // 검증 성공 시 즉시 삭제
            repository.deleteByEmail(key);
            repository.flush(); // 즉시 반영
            return true;
        }
        
        return false;
    }
}
