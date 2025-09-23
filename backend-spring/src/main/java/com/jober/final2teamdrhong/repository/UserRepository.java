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

    // TODO: 계정 통합 로직 구현 시 추가 예정
    // 핸드폰 번호로 사용자 조회 (소셜 회원가입 시 중복 체크, 계정 통합용)
    // Optional<User> findByUserNumber(String userNumber);

    // TODO: 소셜 로그인 기존 사용자 확인 로직 구현 시 추가 예정
    // 이메일과 인증 타입으로 사용자 조회 (중복 소셜 계정 방지, 기존 사용자 확인용)
    // @Query("SELECT u FROM User u JOIN u.userAuths ua WHERE u.userEmail = :email AND ua.authType = :authType")
    // Optional<User> findByUserEmailAndAuthType(@Param("email") String email, @Param("authType") UserAuth.AuthType authType);
}