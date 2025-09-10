package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Integer> {
    /**
     * 이메일로 사용자를 조회합니다. (회원가입 시 중복 체크용)
     * @param email 사용자 이메일
     * @return Optional<User>
     */
    Optional<User> findByUserEmail(String email);
    
    /**
     * N+1 쿼리 방지를 위해 UserAuth와 함께 조회
     * 로그인 시 UserAuth 정보가 필요한 경우 사용
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userAuths WHERE u.userEmail = :email")
    Optional<User> findByUserEmailWithAuth(@Param("email") String email);
}
