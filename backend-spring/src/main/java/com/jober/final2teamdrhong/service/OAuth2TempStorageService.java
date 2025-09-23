package com.jober.final2teamdrhong.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * OAuth2 임시 사용자 정보를 Redis에 저장하고 관리하는 서비스
 * 소셜 로그인 후 핸드폰 번호 입력까지의 임시 기간 동안 사용자 정보를 보관합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TempStorageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;


    /**
     * OAuth2 임시 사용자 정보를 Redis에 저장
     *
     * @param tempUserInfo 저장할 임시 사용자 정보
     * @return 생성된 임시 키
     * @throws BusinessException 저장 실패 시
     */
    public String storeTempUserInfo(OAuth2TempUserInfo tempUserInfo) {
        try {
            // 임시 정보 유효성 검증
            if (!tempUserInfo.isValid()) {
                throw new BusinessException("OAuth2 임시 사용자 정보가 유효하지 않습니다.");
            }

            // 고유한 임시 키 생성
            String tempKey = generateTempKey();

            // Redis에 저장
            redisTemplate.opsForValue().set(tempKey, tempUserInfo, authProperties.getOauth2().getTempInfoExpiryMinutes(), TimeUnit.MINUTES);

            log.info("OAuth2 임시 사용자 정보 저장 완료 - 키: {}, 이메일: {}, 제공자: {}",
                    tempKey, tempUserInfo.email(), tempUserInfo.provider());

            return tempKey;

        } catch (Exception e) {
            log.error("OAuth2 임시 사용자 정보 저장 실패: {}", e.getMessage(), e);
            throw new BusinessException("임시 사용자 정보 저장에 실패했습니다.");
        }
    }

    /**
     * Redis에서 OAuth2 임시 사용자 정보를 조회
     *
     * @param tempKey 임시 키
     * @return 임시 사용자 정보 (없으면 null)
     * @throws BusinessException 조회 실패 시
     */
    public OAuth2TempUserInfo retrieveTempUserInfo(String tempKey) {
        try {
            // 키 형식 검증
            if (!isValidTempKey(tempKey)) {
                throw new BusinessException("유효하지 않은 임시 키 형식입니다.");
            }

            // Redis에서 조회
            Object stored = redisTemplate.opsForValue().get(tempKey);

            if (stored == null) {
                log.warn("OAuth2 임시 사용자 정보를 찾을 수 없음 - 키: {}", tempKey);
                return null;
            }

            OAuth2TempUserInfo tempUserInfo;
            if (stored instanceof OAuth2TempUserInfo) {
                tempUserInfo = (OAuth2TempUserInfo) stored;
            } else {
                // JSON 문자열로 저장된 경우 역직렬화
                tempUserInfo = objectMapper.convertValue(stored, OAuth2TempUserInfo.class);
            }

            // 조회된 정보 유효성 검증
            if (!tempUserInfo.isValid()) {
                log.error("Redis에서 조회된 OAuth2 임시 정보가 유효하지 않음 - 키: {}", tempKey);
                deleteTempUserInfo(tempKey); // 유효하지 않은 정보 삭제
                throw new BusinessException("저장된 임시 정보가 손상되었습니다.");
            }

            log.info("OAuth2 임시 사용자 정보 조회 완료 - 키: {}, 이메일: {}",
                    tempKey, tempUserInfo.email());

            return tempUserInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 임시 사용자 정보 조회 실패: {}", e.getMessage(), e);
            throw new BusinessException("임시 사용자 정보 조회에 실패했습니다.");
        }
    }

    /**
     * Redis에서 OAuth2 임시 사용자 정보를 삭제
     * 회원가입 완료 후 또는 만료된 정보 정리 시 사용
     *
     * @param tempKey 삭제할 임시 키
     * @return 삭제 성공 여부
     */
    public boolean deleteTempUserInfo(String tempKey) {
        try {
            if (!isValidTempKey(tempKey)) {
                log.warn("유효하지 않은 임시 키로 삭제 시도: {}", tempKey);
                return false;
            }

            Boolean deleted = redisTemplate.delete(tempKey);
            boolean success = Boolean.TRUE.equals(deleted);

            if (success) {
                log.info("OAuth2 임시 사용자 정보 삭제 완료 - 키: {}", tempKey);
            } else {
                log.warn("OAuth2 임시 사용자 정보 삭제 실패 (존재하지 않음) - 키: {}", tempKey);
            }

            return success;

        } catch (Exception e) {
            log.error("OAuth2 임시 사용자 정보 삭제 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 임시 정보 만료 시간 연장 (필요 시 사용)
     *
     * @param tempKey 연장할 임시 키
     * @param additionalMinutes 추가할 분 수
     * @return 연장 성공 여부
     */
    public boolean extendTempUserInfo(String tempKey, int additionalMinutes) {
        try {
            if (!isValidTempKey(tempKey)) {
                return false;
            }

            Boolean exists = redisTemplate.hasKey(tempKey);
            if (Boolean.TRUE.equals(exists)) {
                redisTemplate.expire(tempKey, additionalMinutes, TimeUnit.MINUTES);
                log.info("OAuth2 임시 사용자 정보 만료 시간 연장 - 키: {}, 추가 시간: {}분", tempKey, additionalMinutes);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("OAuth2 임시 사용자 정보 만료 시간 연장 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 임시 키의 남은 만료 시간 조회 (초 단위)
     *
     * @param tempKey 조회할 임시 키
     * @return 남은 만료 시간 (초, -1은 키가 존재하지 않음, -2는 만료 시간이 설정되지 않음)
     */
    public long getTempUserInfoTTL(String tempKey) {
        try {
            if (!isValidTempKey(tempKey)) {
                return -1;
            }

            return redisTemplate.getExpire(tempKey, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("OAuth2 임시 사용자 정보 TTL 조회 실패: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 고유한 임시 키 생성
     *
     * @return 생성된 임시 키
     */
    private String generateTempKey() {
        return authProperties.getOauth2().getTempKeyPrefix() + java.util.UUID.randomUUID().toString();
    }

    /**
     * 임시 키 형식 유효성 검증
     *
     * @param tempKey 검증할 임시 키
     * @return 유효성 여부
     */
    private boolean isValidTempKey(String tempKey) {
        String tempKeyPrefix = authProperties.getOauth2().getTempKeyPrefix();
        return tempKey != null &&
               tempKey.startsWith(tempKeyPrefix) &&
               tempKey.length() > tempKeyPrefix.length();
    }
}