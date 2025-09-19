package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface UserAuthRepository extends JpaRepository<UserAuth, Integer> {
}
