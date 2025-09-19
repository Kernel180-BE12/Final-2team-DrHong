package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 조회 (ID로)
     */
    public Optional<User> findById(Integer userId) {
        return userRepository.findById(userId);
    }

    /**
     * 사용자 조회 (이메일로)
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByUserEmail(email);
    }

    /**
     * 사용자 저장
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 사용자 삭제
     */
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }

    /**
     * 모든 사용자 조회 (페이징)
     */
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}