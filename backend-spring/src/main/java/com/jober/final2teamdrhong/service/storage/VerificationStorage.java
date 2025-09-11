package com.jober.final2teamdrhong.service.storage;

import java.util.Optional;
/**
 * 인증 코드를 저장하고 관리하는 모든 저장소의 "역할(Contract)"을 정의하는 인터페이스입니다.
 * 이 인터페이스를 구현하는 클래스는 어떤 기술을 사용하든 동일한 기능을 제공해야 합니다.
 */
public interface VerificationStorage {

    /**
     * 지정된 키(예: 이메일)에 값(예: 인증 코드)을 저장합니다.
     * @param key 저장할 키
     * @param value 저장할 값
     */
    void save(String key, String value);

    /**
     * 지정된 키로 값을 조회합니다.
     * @param key 조회할 키
     * @return 값이 있으면 Optional<String>으로 반환, 없으면 Optional.empty()
     */
    Optional<String> find(String key);

    /**
     * 지정된 키의 값을 삭제합니다.
     * @param key 삭제할 키
     */
    void delete(String key);
    
    /**
     * 인증 코드를 검증하고 즉시 삭제합니다 (일회성 검증)
     * @param key 검증할 키
     * @param expectedValue 예상 값
     * @return 검증 성공 여부
     */
    default boolean validateAndDelete(String key, String expectedValue) {
        Optional<String> actualValue = find(key);
        if (actualValue.isPresent() && actualValue.get().equals(expectedValue)) {
            delete(key);
            return true;
        }
        return false;
    }
}