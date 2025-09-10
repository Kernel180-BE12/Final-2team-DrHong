package com.jober.final2teamdrhong.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fallback 로직을 담당하는 "총괄 매니저" 구현체입니다.
 * Plan A (Redis)를 먼저 시도하고, 실패 시 Plan B (RDB)로 자동 전환합니다.
 */
@Slf4j
@Component
@Primary // Spring이 VerificationStorage를 주입할 때, 이 구현체를 최우선으로 사용하도록 지정
@Profile("!redis-fallback-test") // Redis 폴백 테스트가 아닐 때만 활성화
public class FallbackVerificationStorage implements VerificationStorage {

    private final VerificationStorage primaryStorage;   // Plan A: Redis
    private final VerificationStorage secondaryStorage; // Plan B: RDB

    public FallbackVerificationStorage(
            @Qualifier("redisStorage") VerificationStorage primaryStorage,
            @Qualifier("rdbStorage") VerificationStorage secondaryStorage) {
        this.primaryStorage = primaryStorage;
        this.secondaryStorage = secondaryStorage;
    }

    @Override
    public void save(String key, String value) {
        try {
            primaryStorage.save(key, value);
        } catch (DataAccessException e) {
            log.warn(" 주 저장소(Redis) 장애 발생. 예비 저장소(RDB)로 전환합니다. 에러: {}", e.getMessage());
            secondaryStorage.save(key, value);
        }
    }

    @Override
    public Optional<String> find(String key) {
        try {
            Optional<String> primaryResult = primaryStorage.find(key);
            if (primaryResult.isPresent()) {
                return primaryResult;
            }
        } catch (DataAccessException e) {
            log.warn(" 주 저장소(Redis) 장애 발생. 예비 저장소(RDB)로 전환합니다. 에러: {}", e.getMessage());
        }
        // Primary에서 찾지 못했거나 장애가 발생한 경우 Secondary 확인
        return secondaryStorage.find(key);
    }

    @Override
    public void delete(String key) {
        try {
            primaryStorage.delete(key);
        } catch (DataAccessException e) {
            log.warn(" 주 저장소(Redis) 장애 발생. 예비 저장소(RDB)로 전환합니다. 에러: {}", e.getMessage());
        }
        // 양쪽 모두에서 삭제해야 함 (어디에 저장되었는지 모르므로)
        try {
            secondaryStorage.delete(key);
        } catch (DataAccessException e) {
            log.debug("RDB에서 삭제 시 에러 발생 (정상적일 수 있음): {}", e.getMessage());
        }
    }
}
