package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 이메일 인증 Repository (RDB용)
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
    Optional<EmailVerification> findByEmail(String email);
    
    @Modifying
    @Transactional
    void deleteByEmail(String email);
}
