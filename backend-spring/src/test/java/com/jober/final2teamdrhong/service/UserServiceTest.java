package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        lenient().when(mockUser.getUserId()).thenReturn(1);
        lenient().when(mockUser.getUserName()).thenReturn("테스트유저");
        lenient().when(mockUser.getUserEmail()).thenReturn(testEmail);
        lenient().when(mockUser.getUserNumber()).thenReturn("010-1234-5678");
        lenient().when(mockUser.getUserRole()).thenReturn(User.UserRole.USER);
        lenient().when(mockUser.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
        lenient().when(mockUser.getUpdatedAt()).thenReturn(java.time.LocalDateTime.now());
        lenient().when(mockUser.getUserAuths()).thenReturn(java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("성공: ID로 사용자 조회")
    void findById_success() {
        // given
        Integer userId = 1;
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        // when
        Optional<User> result = userService.findById(userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockUser);
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("성공: 이메일로 사용자 조회")
    void findByEmail_success() {
        // given
        given(userRepository.findByUserEmail(testEmail)).willReturn(Optional.of(mockUser));

        // when
        Optional<User> result = userService.findByEmail(testEmail);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockUser);
        then(userRepository).should().findByUserEmail(testEmail);
    }

    @Test
    @DisplayName("성공: 사용자 저장")
    void saveUser_success() {
        // given
        given(userRepository.save(mockUser)).willReturn(mockUser);

        // when
        User result = userService.saveUser(mockUser);

        // then
        assertThat(result).isEqualTo(mockUser);
        then(userRepository).should().save(mockUser);
    }

    @Test
    @DisplayName("성공: 사용자 삭제")
    void deleteUser_success() {
        // given
        Integer userId = 1;

        // when
        userService.deleteUser(userId);

        // then
        then(userRepository).should().deleteById(userId);
    }

    @Test
    @DisplayName("성공: 모든 사용자 조회 (페이징)")
    void findAllUsers_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(List.of(mockUser), pageable, 1);
        given(userRepository.findAll(pageable)).willReturn(expectedPage);

        // when
        Page<User> result = userService.findAllUsers(pageable);

        // then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        then(userRepository).should().findAll(pageable);
    }
}