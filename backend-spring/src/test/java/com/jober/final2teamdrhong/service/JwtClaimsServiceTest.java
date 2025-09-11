package com.jober.final2teamdrhong.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐싱 기능 테스트
 * Redis 캐시 설정이 올바르게 작동하는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 캐시 초기화 - userInfo 캐시가 있다면 초기화
        if (cacheManager.getCache("userInfo") != null) {
            cacheManager.getCache("userInfo").clear();
        }
    }

    @Test
    @DisplayName("CacheManager가 정상적으로 주입되는지 확인")
    void shouldInjectCacheManager() {
        // when & then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).isNotEmpty();
    }

    @Test
    @DisplayName("캐시에 값 저장 및 조회 테스트")
    void shouldStoreAndRetrieveFromCache() {
        // given
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            // userInfo 캐시가 설정되지 않은 경우 테스트 스킵
            return;
        }
        
        String key = "test@example.com:1";
        String value = "testValue";

        // when
        cache.put(key, value);
        Cache.ValueWrapper cachedValue = cache.get(key);

        // then
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("캐시 무효화 테스트")
    void shouldEvictFromCache() {
        // given
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            return;
        }
        
        String key = "test@example.com:1";
        String value = "testValue";
        cache.put(key, value);
        assertThat(cache.get(key)).isNotNull();

        // when
        cache.evict(key);

        // then
        assertThat(cache.get(key)).isNull();
    }

    @Test
    @DisplayName("모든 캐시 초기화 테스트")
    void shouldClearAllCache() {
        // given
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            return;
        }
        
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertThat(cache.get("key1")).isNotNull();
        assertThat(cache.get("key2")).isNotNull();

        // when
        cache.clear();

        // then
        assertThat(cache.get("key1")).isNull();
        assertThat(cache.get("key2")).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 캐시 키 조회 테스트")
    void shouldReturnNullForNonExistentKey() {
        // given
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            return;
        }

        // when
        Cache.ValueWrapper result = cache.get("nonexistent-key");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("다양한 키 타입 저장 테스트")
    void shouldHandleDifferentKeyTypes() {
        // given
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            return;
        }

        // when & then
        cache.put("string-key", "string-value");
        cache.put(123, "integer-key-value");
        cache.put("email:1", "user-data");
        
        assertThat(cache.get("string-key")).isNotNull();
        assertThat(cache.get(123)).isNotNull();
        assertThat(cache.get("email:1")).isNotNull();
    }

    @Test
    @DisplayName("특정 키만 무효화되고 다른 키는 유지되는지 테스트")
    void shouldEvictOnlySpecificKey() {
        // given
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            return;
        }
        
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertThat(cache.get("key1")).isNotNull();
        assertThat(cache.get("key2")).isNotNull();

        // when
        cache.evict("key1");

        // then
        assertThat(cache.get("key1")).isNull(); // 특정 키만 삭제
        assertThat(cache.get("key2")).isNotNull(); // 다른 키는 유지
    }
}