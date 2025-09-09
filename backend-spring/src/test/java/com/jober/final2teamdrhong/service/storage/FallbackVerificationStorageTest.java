package com.jober.final2teamdrhong.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

/**
 * FallbackVerificationStorage 단위 테스트
 * Redis -> RDB 폴백 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class FallbackVerificationStorageTest {

    @Mock
    private VerificationStorage primaryStorage; // Redis

    @Mock
    private VerificationStorage secondaryStorage; // RDB

    private FallbackVerificationStorage fallbackStorage;

    @BeforeEach
    void setUp() {
        fallbackStorage = new FallbackVerificationStorage(primaryStorage, secondaryStorage);
    }

    @Test
    @DisplayName("저장 성공: Primary(Redis) 정상 동작")
    void save_success_primary() {
        // given
        String key = "test@example.com";
        String value = "123456";

        // when
        fallbackStorage.save(key, value);

        // then
        then(primaryStorage).should().save(key, value);
        then(secondaryStorage).should(never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("저장 폴백: Primary(Redis) 실패 시 Secondary(RDB)로 전환")
    void save_fallback_to_secondary() {
        // given
        String key = "test@example.com";
        String value = "123456";
        doThrow(new DataAccessException("Redis connection failed") {}).when(primaryStorage).save(key, value);

        // when
        fallbackStorage.save(key, value);

        // then
        then(primaryStorage).should().save(key, value);
        then(secondaryStorage).should().save(key, value);
    }

    @Test
    @DisplayName("조회 성공: Primary(Redis)에서 데이터 발견")
    void find_success_primary() {
        // given
        String key = "test@example.com";
        String expectedValue = "123456";
        given(primaryStorage.find(key)).willReturn(Optional.of(expectedValue));

        // when
        Optional<String> result = fallbackStorage.find(key);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedValue);
        then(primaryStorage).should().find(key);
        then(secondaryStorage).should(never()).find(anyString());
    }

    @Test
    @DisplayName("조회 폴백: Primary(Redis)에 데이터 없음, Secondary(RDB)에서 조회")
    void find_fallback_to_secondary() {
        // given
        String key = "test@example.com";
        String expectedValue = "123456";
        given(primaryStorage.find(key)).willReturn(Optional.empty());
        given(secondaryStorage.find(key)).willReturn(Optional.of(expectedValue));

        // when
        Optional<String> result = fallbackStorage.find(key);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedValue);
        then(primaryStorage).should().find(key);
        then(secondaryStorage).should().find(key);
    }

    @Test
    @DisplayName("조회 폴백: Primary(Redis) 장애 시 Secondary(RDB)로 전환")
    void find_fallback_on_primary_failure() {
        // given
        String key = "test@example.com";
        String expectedValue = "123456";
        given(primaryStorage.find(key)).willThrow(new DataAccessException("Redis connection failed") {});
        given(secondaryStorage.find(key)).willReturn(Optional.of(expectedValue));

        // when
        Optional<String> result = fallbackStorage.find(key);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedValue);
        then(primaryStorage).should().find(key);
        then(secondaryStorage).should().find(key);
    }

    @Test
    @DisplayName("삭제: 양쪽 저장소 모두에서 삭제 시도")
    void delete_from_both_storages() {
        // given
        String key = "test@example.com";

        // when
        fallbackStorage.delete(key);

        // then
        then(primaryStorage).should().delete(key);
        then(secondaryStorage).should().delete(key);
    }

    @Test
    @DisplayName("삭제 부분 실패: Primary 실패해도 Secondary는 삭제 시도")
    void delete_primary_fails_but_secondary_succeeds() {
        // given
        String key = "test@example.com";
        doThrow(new DataAccessException("Redis connection failed") {}).when(primaryStorage).delete(key);

        // when
        fallbackStorage.delete(key);

        // then
        then(primaryStorage).should().delete(key);
        then(secondaryStorage).should().delete(key);
    }
}